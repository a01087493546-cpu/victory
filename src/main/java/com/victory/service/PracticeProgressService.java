package com.victory.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.victory.dto.PracticeProgressRequest;
import com.victory.dto.PracticeProgressResponse;
import com.victory.entity.PracticeProgress;
import com.victory.entity.User;
import com.victory.repository.PracticeProgressRepository;
import com.victory.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeProgressService {

    private final PracticeProgressRepository practiceProgressRepository;
    private final UserRepository userRepository;

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

        return PracticeProgressResponse.from(savedProgress);
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
    }
}