package com.victory.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.victory.dto.PracticeProgressRequest;
import com.victory.dto.PracticeProgressResponse;
import com.victory.dto.StudentStatsResponse;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.PracticeProgress;
import com.victory.entity.User;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.PracticeProgressRepository;
import com.victory.repository.UserRepository;
import com.victory.service.PracticeReadingRewardService.RewardResult;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeProgressService {

    private final PracticeProgressRepository practiceProgressRepository;
    private final UserRepository userRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassReadingBookRepository classReadingBookRepository;
    private final PracticeReadingRewardService practiceReadingRewardService;

    /*
     * 학생 진행 상태 조회
     * 진행 상태가 없으면 기본값으로 새로 생성한다.
     */
    @Transactional
    public PracticeProgressResponse getProgress(Long studentId) {

        User student = findStudent(studentId);

        PracticeProgress progress = practiceProgressRepository
            .findByStudent_Id(studentId)
            .orElseGet(() -> createAndSaveProgress(student));

        if (isPracticeCompleted(progress)) {
            Long classReadingBookId = findCurrentClassReadingBookId(studentId);
            RewardResult rewardResult =
                practiceReadingRewardService.getRewardState(student, classReadingBookId);

            return PracticeProgressResponse.from(
                progress,
                false,
                rewardResult.isRewardAlreadyGranted(),
                StudentStatsResponse.from(rewardResult.getStats()));
        }

        return PracticeProgressResponse.from(progress);
    }

    /*
     * 프론트에서 받은 진행 상태를 저장한다.
     * 기존 데이터가 없으면 새로 만든다.
     */
    @Transactional
    public PracticeProgressResponse saveProgress(
            Long studentId,
            PracticeProgressRequest request) {

        User student = findStudent(studentId);

        PracticeProgress progress = practiceProgressRepository
            .findByStudent_Id(studentId)
            .orElseGet(() -> createProgress(student));

        updateProgress(progress, request);

        PracticeProgress savedProgress =
            practiceProgressRepository.save(progress);

        RewardResult rewardResult = maybeGrantPracticeCompleteReward(
            student,
            savedProgress);

        return PracticeProgressResponse.from(
            savedProgress,
            rewardResult.isRewardGranted(),
            rewardResult.isRewardAlreadyGranted(),
            StudentStatsResponse.from(rewardResult.getStats()));
    }

    /*
     * users 테이블에서 학생을 찾는다.
     */
    private User findStudent(Long studentId) {

        User user = userRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException(
                "학생을 찾을 수 없습니다. studentId=" + studentId
            ));

        if (!"student".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException(
                "학생 계정만 연습읽기 진행 상태를 저장할 수 있습니다."
            );
        }

        return user;
    }

    /*
     * 기본 진행 상태를 만든 뒤 DB에 저장한다.
     */
    private PracticeProgress createAndSaveProgress(User student) {

        PracticeProgress progress = createProgress(student);

        return practiceProgressRepository.save(progress);
    }

    /*
     * 모든 진행 상태가 false인 기본 객체를 만든다.
     */
    private PracticeProgress createProgress(User student) {

        PracticeProgress progress = new PracticeProgress();

        progress.setStudent(student);
        progress.setBookSelected(false);
        progress.setBeforeDone(false);
        progress.setClassReadDone(false);
        progress.setAfterDone(false);
        progress.setDuringTypeProgress(PracticeProgress.createDefaultDuringProgress());
        progress.setAfterTypeProgress(PracticeProgress.createDefaultAfterProgress());

        return progress;
    }

    /*
     * 프론트에서 전달된 값만 수정한다.
     * null인 값은 기존 상태를 그대로 유지한다.
     */
    private void updateProgress(
            PracticeProgress progress,
            PracticeProgressRequest request) {

        if (request.getBookSelected() != null) {
            progress.setBookSelected(request.getBookSelected());
        }

        if (request.getBeforeDone() != null) {
            progress.setBeforeDone(request.getBeforeDone());
        }

        if (request.getClassReadDone() != null) {
            progress.setClassReadDone(request.getClassReadDone());
        }

        if (request.getAfterDone() != null) {
            progress.setAfterDone(request.getAfterDone());
        }

        if (request.getDuringTypeProgress() != null) {

            Map<String, Boolean> mergedProgress =
                PracticeProgress.createDefaultDuringProgress();

            if (progress.getDuringTypeProgress() != null) {
                mergedProgress.putAll(
                    progress.getDuringTypeProgress()
                );
            }

            request.getDuringTypeProgress().forEach(
                (type, done) -> {
                    if (mergedProgress.containsKey(type)
                            && done != null) {
                        mergedProgress.put(type, done);
                    }
                }
            );

            progress.setDuringTypeProgress(mergedProgress);
        }

        if (request.getAfterTypeProgress() != null) {

            Map<String, Boolean> mergedAfterProgress =
                PracticeProgress.createDefaultAfterProgress();

            if (progress.getAfterTypeProgress() != null) {
                mergedAfterProgress.putAll(
                    progress.getAfterTypeProgress()
                );
            }

            request.getAfterTypeProgress().forEach(
                (type, done) -> {
                    if (mergedAfterProgress.containsKey(type)
                            && done != null) {
                        mergedAfterProgress.put(type, done);
                    }
                }
            );

            progress.setAfterTypeProgress(mergedAfterProgress);
        }
    }

    private RewardResult maybeGrantPracticeCompleteReward(
            User student,
            PracticeProgress progress) {

        if (!isPracticeCompleted(progress)) {
            return new RewardResult(false, false, null);
        }

        Long classReadingBookId = findCurrentClassReadingBookId(student.getId());

        if (classReadingBookId == null) {
            return new RewardResult(false, false, null);
        }

        return practiceReadingRewardService
            .grantPracticeCompleteRewardOnce(student, classReadingBookId);
    }

    private boolean isPracticeCompleted(PracticeProgress progress) {
        /*
         * afterDone은 간추리기 GOOD + 질문/답 검증 + 공유 Summary(PENDING)
         * 저장이 모두 성공한 최종 트랜잭션에서만 true가 된다. 예전에는
         * 여기에 읽기 중 5유형의 과거 플래그까지 다시 요구해서, 정상적인
         * 공유 글과 afterDone이 저장됐는데도 +8 보상과 개별읽기 해금만
         * 누락되는 계정이 생겼다. 최종 완료의 단일 원본을 afterDone으로
         * 통일한다. 단계별 잠금은 각 화면의 기존 조건이 계속 담당한다.
         */
        return Boolean.TRUE.equals(progress.getAfterDone());
    }

    private Long findCurrentClassReadingBookId(Long studentId) {
        return classStudentRepository.findByStudentId(studentId)
            .map(ClassStudent::getSchoolClass)
            .flatMap(schoolClass ->
                classReadingBookRepository.findBySchoolClassId(schoolClass.getId()))
            .map(ClassReadingBook::getId)
            .orElse(null);
    }
}
