package com.victory.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.victory.entity.ClassReadingBook;
import com.victory.entity.ContentLike;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ContentLikeRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.SummaryRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** 심사 학급의 온책읽기 활동 예시를 기존 API가 읽을 수 있는 형태로 멱등 생성한다. */
@Component
@Order(4)
@RequiredArgsConstructor
public class DemoClassActivityInitializer implements ApplicationRunner {
    public static final String DEMO_MARKER = "review-class-activity-v1";
    private static final int DEMO_CURRENT_PAGE = 38;

    private static final List<QuestionSeed> QUESTIONS = List.of(
        new QuestionSeed("ss01", "direct", "주인공이 보물을 찾기 위해 가장 먼저 준비한 것은 무엇인가요?", "보물을 찾으러 갈 때 필요한 물건을 준비했습니다.", -3),
        new QuestionSeed("ss01", "direct", "주인공은 보물을 찾으러 누구와 함께 갔나요?", "친구와 함께 찾아갔습니다.", -2),
        new QuestionSeed("ss01", "infer", "주인공은 왜 끝까지 보물을 찾으려고 했을까요?", "자신에게 정말 소중한 것이 무엇인지 알고 싶었기 때문이라고 생각합니다.", -1),
        new QuestionSeed("ss01", "infer", "주인공이 친구의 도움을 받았을 때 어떤 마음이었을까요?", "혼자가 아니라는 생각에 안심되고 고마웠을 것 같습니다.", 0),
        new QuestionSeed("ss01", "opinion", "내가 주인공이라면 보물을 찾는 일을 끝까지 계속했을까요?", "힘들어도 궁금해서 끝까지 찾아봤을 것 같습니다.", 0),
        new QuestionSeed("ss01", "opinion", "내가 생각하는 진짜 보물은 무엇인가요?", "가족이나 친구와 함께한 좋은 기억도 보물이라고 생각합니다.", 1),
        new QuestionSeed("ss01", "connect", "나도 소중한 물건을 잃어버렸다가 다시 찾은 적이 있나요?", "좋아하는 필통을 잃어버렸다가 교실에서 다시 찾은 적이 있습니다.", 2),
        new QuestionSeed("ss01", "connect", "친구의 도움을 받아 어려운 일을 해결했던 경험이 있나요?", "어려운 수학 문제를 친구가 설명해 줘서 해결한 적이 있습니다.", -3),
        new QuestionSeed("demo_student_02", "direct", "주인공이 어려운 상황에서도 계속 앞으로 갈 수 있었던 까닭은 무엇일까요?", "A. 친구의 도움과 응원이 있었기 때문이다. / B. 집에 빨리 가고 싶었기 때문이다. (정답 A)", 0),
        new QuestionSeed("demo_student_02", "opinion", "책을 읽고 보물에 대한 생각이 어떻게 달라졌나요?", "처음에는 보물이 물건이라고 생각했는데 사람이나 기억도 보물이 될 수 있다고 생각했어.", -1),
        new QuestionSeed("demo_student_03", "direct", "내가 주인공이라면 어떤 것을 나만의 보물이라고 고를까요?", "A. 가족과 찍은 사진 / B. 아주 비싼 장난감 (선택 A)", -1),
        new QuestionSeed("demo_student_03", "connect", "나에게 오래 기억하고 싶은 순간은 무엇인가요?", "가족과 같이 놀러 갔던 날을 오래 기억하고 싶어.", -2),
        new QuestionSeed("demo_student_04", "direct", "주인공이 보물을 찾을 때 필요한 것은 무엇이었나요?", "A. 포기하지 않는 용기 / B. 비싼 장난감 (정답 A)", -2),
        new QuestionSeed("demo_student_04", "infer", "주인공은 길을 잃었을 때 왜 다시 도전했을까요?", "진짜 보물이 무엇인지 꼭 알고 싶었기 때문인 것 같아.", 0),
        new QuestionSeed("demo_student_05", "direct", "주인공이 보물을 찾아가면서 가장 중요하게 깨달은 것은 무엇일까요?", "A. 비싼 물건이 중요하다. / B. 나에게 소중한 것을 아는 것이 중요하다. (정답 B)", 0),
        new QuestionSeed("demo_student_05", "opinion", "보물은 꼭 비싼 물건이어야 할까요?", "나는 보물이 꼭 비싼 물건일 필요는 없다고 생각해. 오래 기억하고 싶은 것도 보물이 될 수 있어.", -1),
        new QuestionSeed("demo_student_06", "direct", "친구가 주인공에게 해 준 일은 무엇인가요?", "A. 함께 방법을 찾아 주었다. / B. 혼자 집으로 돌아갔다. (정답 A)", -1),
        new QuestionSeed("demo_student_06", "connect", "친구가 어려울 때 나는 어떻게 도울 수 있을까요?", "친구가 도와주는 장면이 기억에 남았어. 나도 친구가 어려울 때 도와주고 싶어.", -2),
        new QuestionSeed("demo_student_07", "direct", "이 이야기에서 진짜 보물에 가까운 것은 무엇일까요?", "A. 소중한 사람과 기억 / B. 가장 비싼 물건 (정답 A)", -2),
        new QuestionSeed("demo_student_07", "opinion", "내가 가장 소중하게 생각하는 것은 무엇인가요?", "내가 가장 소중하게 생각하는 것은 가족과 같이 여행 갔던 사진이야.", 0),
        new QuestionSeed("demo_student_08", "direct", "주인공이 가장 두려웠던 순간은 언제였을까요?", "A. 어두운 동굴에서 길을 잃었을 때 / B. 보물을 찾고 집으로 돌아왔을 때 (정답 A)", 0),
        new QuestionSeed("demo_student_08", "opinion", "주인공의 어떤 모습이 기억에 남았나요?", "주인공이 포기하지 않은 점이 멋있었어. 나라면 중간에 힘들어서 고민했을 것 같아.", -1)
    );

    private static final List<SummarySeed> SUMMARIES = List.of(
        new SummarySeed("ss01", "주인공은 보물을 찾기 위해 모험을 떠났습니다. 어려운 일이 있었지만 포기하지 않고 친구의 도움을 받아 계속 나아갔습니다. 마지막에는 진짜 보물은 소중한 사람들과 함께한 경험이라는 것을 깨달았습니다.", 5),
        new SummarySeed("demo_student_02", "주인공은 처음에는 특별한 보물을 찾으려고 했지만 보물을 찾는 과정에서 친구와 함께한 시간도 소중하다는 것을 알게 되었습니다.", 3),
        new SummarySeed("demo_student_03", "보물을 찾으러 간 주인공은 어려운 일이 생겨도 포기하지 않았습니다. 마지막에는 자신이 소중하게 생각하는 것이 진짜 보물이 될 수 있다는 것을 깨달았습니다.", 6),
        new SummarySeed("demo_student_05", "주인공은 친구와 힘을 합쳐 보물을 찾아갑니다. 그 과정에서 도움을 주고받으며 혼자보다 함께하는 것이 좋다는 것을 알게 됩니다.", 2),
        new SummarySeed("demo_student_07", "이 이야기는 보물을 찾아가는 이야기입니다. 주인공은 여러 일을 겪으며 비싼 물건만 보물이 아니라 소중한 사람과 기억도 보물이 될 수 있다는 것을 알게 됩니다.", 4),
        new SummarySeed("demo_student_08", "주인공은 끝까지 포기하지 않고 보물을 찾아갑니다. 나는 주인공이 용기를 내어 계속 도전한 모습이 기억에 남았습니다.", 3)
    );

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassReadingBookRepository classReadingBookRepository;
    private final ResponseRepository responseRepository;
    private final SummaryRepository summaryRepository;
    private final ContentLikeRepository contentLikeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User teacher = userRepository.findByLoginId("tt11").orElse(null);
        if (teacher == null || !Boolean.TRUE.equals(teacher.getDemoAccount())) return;
        SchoolClass schoolClass = schoolClassRepository.findByTeacherId(teacher.getId()).orElse(null);
        if (schoolClass == null) return;
        ClassReadingBook book = classReadingBookRepository.findBySchoolClassId(schoolClass.getId()).orElse(null);
        if (book == null) return;

        if ("나만의 보물 찾기".equals(book.getBookTitle()) && book.getTotalPages() != null && book.getTotalPages() == 52
                && !Integer.valueOf(DEMO_CURRENT_PAGE).equals(book.getCurrentPage())) {
            book.setCurrentPage(DEMO_CURRENT_PAGE);
            classReadingBookRepository.save(book);
        }

        for (int index = 0; index < QUESTIONS.size(); index++) ensureQuestion(book, QUESTIONS.get(index), index);

        List<User> demoStudents = new ArrayList<>();
        for (String loginId : List.of("ss01", "demo_student_02", "demo_student_03", "demo_student_04", "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08")) {
            userRepository.findByLoginId(loginId).filter(user -> Boolean.TRUE.equals(user.getDemoAccount())).ifPresent(demoStudents::add);
        }
        for (SummarySeed seed : SUMMARIES) ensureSummary(book, seed, demoStudents);
    }

    private void ensureQuestion(ClassReadingBook book, QuestionSeed seed, int index) {
        User student = userRepository.findByLoginId(seed.loginId()).orElse(null);
        if (student == null || !Boolean.TRUE.equals(student.getDemoAccount())) return;
        Response existing = responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                student.getId(), "class", "answer", "during").stream()
            .filter(item -> isDemoSeed(item) && Integer.valueOf(index).equals(asInteger(item.getExtraData().get("demoSeedIndex"))))
            .findFirst().orElse(null);

        if (existing != null) {
            /*
             * 질문 문구가 바뀌어도 demoSeedIndex로 같은 행을 갱신한다.
             * 질문 문자열로 찾으면 예전 seed가 남은 채 새 행이 추가된다.
             */
            Map<String, Object> updatedExtra = new HashMap<>(existing.getExtraData());
            updatedExtra.put("demoDayOffset", seed.dayOffset());
            updatedExtra.put("questionType", seed.type());
            updatedExtra.put("question", seed.question());
            existing.setContent(seed.answer());
            existing.setExtraData(updatedExtra);
            responseRepository.save(existing);
            return;
        }

        Response response = new Response();
        response.setStudent(student);
        response.setMode("class");
        response.setContentType("answer");
        response.setStage("during");
        response.setContent(seed.answer());
        response.setPassed(true);
        response.setStatus("approved");
        Map<String, Object> extra = new HashMap<>();
        extra.put("demoSeed", DEMO_MARKER);
        extra.put("demoSeedIndex", index);
        extra.put("demoDayOffset", seed.dayOffset());
        extra.put("activityType", "book_thought");
        extra.put("questionType", seed.type());
        extra.put("question", seed.question());
        extra.put("classReadingBookId", book.getId());
        extra.put("approvalStatus", "APPROVED");
        response.setExtraData(extra);
        responseRepository.save(response);
    }

    private void ensureSummary(ClassReadingBook book, SummarySeed seed, List<User> demoStudents) {
        User student = userRepository.findByLoginId(seed.loginId()).orElse(null);
        if (student == null || !Boolean.TRUE.equals(student.getDemoAccount())) return;
        Summary summary = summaryRepository.findByStudent_IdAndClassReadingBookId(student.getId(), book.getId()).orElse(null);
        if (summary == null) {
            summary = new Summary();
            summary.setStudent(student);
            summary.setClassReadingBookId(book.getId());
            summary.setBookType("story");
            summary.setSummaryText(seed.text());
            summary.setIsShared(true);
            summary.setStatus("approved");
            summary.setAiPassed(true);
            summary = summaryRepository.save(summary);
        } else if ("ss01".equals(seed.loginId()) && !seed.text().equals(summary.getSummaryText())) {
            /*
             * 대표학생 간추리기의 테스트용 본문만 자연스러운 seed 문장으로
             * 멱등 갱신한다. 기존 Summary 행을 그대로 UPDATE하므로 id,
             * createdAt, status, bookType, 공유 여부와 ContentLike 관계는
             * 바뀌지 않으며 다른 학생 seed에도 영향을 주지 않는다.
             */
            summary.setSummaryText(seed.text());
            summary = summaryRepository.save(summary);
        }
        for (int i = 0; i < Math.min(seed.likeCount(), demoStudents.size()); i++) {
            User liker = demoStudents.get(i);
            if (contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(liker.getId(), "summary", summary.getId()).isPresent()) continue;
            ContentLike like = new ContentLike();
            like.setStudent(liker);
            like.setContentType("summary");
            like.setContentId(summary.getId());
            contentLikeRepository.save(like);
        }
    }

    private boolean isDemoSeed(Response response) {
        return response.getExtraData() != null && DEMO_MARKER.equals(response.getExtraData().get("demoSeed"));
    }

    private Integer asInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private record QuestionSeed(String loginId, String type, String question, String answer, int dayOffset) {}
    private record SummarySeed(String loginId, String text, int likeCount) {}
}
