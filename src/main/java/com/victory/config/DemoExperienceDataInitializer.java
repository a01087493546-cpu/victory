package com.victory.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.victory.entity.Book;
import com.victory.entity.BookRecommendation;
import com.victory.entity.ContentLike;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.BookRecommendationRepository;
import com.victory.repository.BookRepository;
import com.victory.repository.ContentLikeRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SummaryRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** 심사 학급 공유 화면과 독서 보관함에서 사용할 고정 예시를 멱등 생성한다. */
@Component
@Order(2)
@RequiredArgsConstructor
public class DemoExperienceDataInitializer implements ApplicationRunner {
    private static final String DEMO_MARKER = "review-demo-v1";
    private static final String CONTENT_TYPE_BOOK_RECOMMENDATION = "book_recommendation";
    private static final String LEGACY_REASON_PREFIX = "[심사 체험 예시] ";
    private static final List<String> DEMO_STUDENT_LOGIN_IDS = List.of(
        "ss01", "demo_student_02", "demo_student_03", "demo_student_04",
        "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08");
    private static final List<BookChatSeed> BOOK_CHAT_SEEDS = List.of(
        new BookChatSeed("ss01", "잎싹의 선택",
            "잎싹은 닭장을 나와 자유롭게 살고 싶어 합니다.\n하지만 밖에서는 혼자 위험한 일을 헤쳐 나가야 합니다.",
            "안전한 닭장으로 다시 돌아간다.", "힘들어도 밖에서 자유롭게 살아간다.", "B",
            List.of(
                new BookChatReplySeed("demo_student_02", "B", "나는 B를 고를래. 힘들어도 내가 하고 싶은 일을 해 보고 싶어."),
                new BookChatReplySeed("demo_student_03", "A", "나는 A도 고민될 것 같아. 밖에서 혼자 지내는 건 무서울 것 같아."))),
        new BookChatSeed("demo_student_02", "노든의 약속",
            "노든은 힘든 일을 겪은 뒤에도 다른 존재와 함께 길을 계속 가야 하는 상황을 만납니다.",
            "혼자서 조용히 살아간다.", "힘들어도 새로운 친구와 함께 길을 간다.", "B",
            List.of(
                new BookChatReplySeed("demo_student_05", "B", "나는 B야. 혼자 있으면 더 외로울 것 같아."),
                new BookChatReplySeed("demo_student_04", "B", "나도 B인데 처음에는 조금 무서울 것 같아."))),
        new BookChatSeed("demo_student_03", "나무의 마지막 선물",
            "나무는 자신이 가진 것을 소년에게 계속 나누어 줍니다.",
            "친구가 원하면 내가 가진 것을 계속 나누어 준다.", "친구도 중요하지만 내가 필요한 것은 남겨 둔다.", "B",
            List.of(
                new BookChatReplySeed("demo_student_06", "B", "나는 B를 고를래. 나도 힘들어지면 계속 줄 수 없을 것 같아."),
                new BookChatReplySeed("demo_student_07", "B", "나는 A도 좋은 것 같지만 너무 많이 주면 나무가 힘들 것 같아."))),
        new BookChatSeed("demo_student_04", "비밀을 발견했다면",
            "도서관에서 아무도 모르는 수상한 비밀을 발견했다고 생각해 봅니다.",
            "혼자서 비밀을 더 조사해 본다.", "친구에게 바로 알려 같이 조사한다.", "B",
            List.of(
                new BookChatReplySeed("demo_student_08", "B", "나는 B야. 혼자 하면 무서우니까 친구랑 같이 할래."),
                new BookChatReplySeed("ss01", "A", "나는 A도 해 보고 싶어. 먼저 조금 알아본 다음 친구한테 말할 것 같아."))),
        new BookChatSeed("demo_student_05", "건강 습관 하나",
            "몸을 건강하게 만들기 위해 매일 한 가지 습관만 꼭 지킬 수 있다고 생각해 봅니다.",
            "매일 운동하기", "매일 충분히 잠자기", "B",
            List.of(
                new BookChatReplySeed("demo_student_03", "A", "나는 A! 운동하고 나면 몸이 더 튼튼해질 것 같아."),
                new BookChatReplySeed("demo_student_02", "B", "나는 B야. 잠을 못 자면 다음 날 너무 피곤해."))),
        new BookChatSeed("demo_student_06", "강아지똥의 선택",
            "강아지똥은 자신이 아무 쓸모가 없다고 생각하지만,\n민들레꽃을 피우는 데 도움이 될 수 있다는 것을 알게 됩니다.",
            "지금 모습이 싫어서 계속 속상해한다.", "내가 할 수 있는 일을 찾아본다.", "B",
            List.of(
                new BookChatReplySeed("demo_student_05", "B", "나는 B야. 처음에는 몰라도 내가 잘하는 게 있을 수 있을 것 같아."),
                new BookChatReplySeed("demo_student_04", "B", "나도 B를 고를래. 한번은 해 봐야 알 수 있을 것 같아."))),
        new BookChatSeed("demo_student_07", "새 책을 발견하면",
            "책을 아주 좋아하는 여우 앞에 처음 보는 재미있는 책이 놓여 있습니다.",
            "바로 읽기 시작한다.", "지금 읽던 책을 다 읽고 새 책을 읽는다.", "A",
            List.of(
                new BookChatReplySeed("demo_student_08", "A", "나는 A! 재미있어 보이면 바로 읽어 보고 싶어."),
                new BookChatReplySeed("demo_student_06", "B", "나는 B야. 읽던 책을 끝까지 읽고 싶어."))),
        new BookChatSeed("demo_student_08", "글을 배울 기회",
            "글을 배우는 것이 쉽지 않지만,\n글을 알게 되면 내 생각을 다른 사람에게 전할 수 있습니다.",
            "어렵더라도 끝까지 글을 배운다.", "너무 어렵다면 다른 사람이 대신 읽고 써 주게 한다.", "A",
            List.of(
                new BookChatReplySeed("ss01", "A", "나는 A야. 내가 직접 편지를 쓸 수 있으면 좋을 것 같아."),
                new BookChatReplySeed("demo_student_07", "A", "나도 A를 고를래. 처음에는 어려워도 계속 하면 늘 것 같아.")))
    );
    /*
     * "책보관함" 상세와 "책 추천하기 -> 질문 선택하기" 화면에서 완독 책마다
     * 읽기 전/읽는 중 후보 질문이 여러 개 보이도록 심사 체험용 완독 책
     * 3권(마당을 나온 암탉/푸른 사자 와니니/세계를 바꾸는 착한 기술)에만
     * 한정해 채워 두는 예시다. ensureAfterQuestions와 달리 책 제목별로
     * 내용이 서로 달라 Example 레코드에 담지 않고 별도 맵으로 관리한다.
     */
    private static final Map<String, List<QA>> BEFORE_QUESTIONS = Map.of(
        "마당을 나온 암탉", List.of(
            new QA("잎싹은 왜 닭장을 떠나고 싶어 했을까요?", "자유롭게 살며 자기 알을 품고 싶었기 때문이에요.", "title"),
            new QA("표지 그림을 보고 무엇을 예상했나요?", "표지에 닭이 알을 품고 있는 그림을 보고, 암탉이 알을 지키려고 애쓰는 이야기일 것 같다고 예상했어요.", "picture"),
            new QA("차례를 보고 궁금했던 점은 무엇인가요?", "차례에 '마당을 나오다', '초록머리' 같은 말이 있어서 마당을 나온 뒤 누구를 만나게 되는지 궁금했어요.", "contents")),
        "푸른 사자 와니니", List.of(
            new QA("표지 속 와니니는 어디를 향해 가고 있을까요?", "새로운 무리와 안전하게 살 곳을 찾아가고 있을 것 같아요.", "picture"),
            new QA("제목을 보고 무엇이 궁금했나요?", "'푸른 사자'라는 말이 진짜 사자 색깔인지, 와니니라는 이름을 가진 사자가 어떤 사자인지 궁금했어요.", "title"),
            new QA("책을 훑어보고 궁금했던 점은 무엇인가요?", "책을 넘겨 보다가 초원과 무리 이야기가 많이 나와서, 와니니가 다시 무리로 돌아갈 수 있을지 궁금했어요.", "skim")),
        "세계를 바꾸는 착한 기술", List.of(
            new QA("착한 기술은 왜 필요하다는 내용이 나올까요?", "생활이 불편한 사람과 환경을 함께 돕기 위해 필요하다는 내용일 것 같아요.", "contents"),
            new QA("제목을 보고 무엇이 궁금했나요?", "'착한 기술'이 어떤 기술인지, 세계를 어떻게 바꾼다는 것인지 궁금했어요.", "title"),
            new QA("표지 그림을 보고 무엇을 예상했나요?", "표지에 여러 발명품 그림이 있어서, 사람들을 도와주는 물건들을 소개하는 책일 것 같다고 예상했어요.", "picture"))
    );
    private static final Map<String, List<QA>> DURING_QUESTIONS = Map.of(
        "마당을 나온 암탉", List.of(
            new QA("잎싹이 가장 바랐던 것은 무엇이었나요?", "자기 알을 품어 병아리를 키우는 것이었어요.", "find"),
            new QA("잎싹이 초록머리를 자기 자식처럼 키우기로 한 까닭은 무엇일까요?", "혼자 알을 품었지만 진짜 자기 아기가 아니어도, 잎싹이 그 알을 지켜 준 마음이 진짜 사랑이 되었기 때문이라고 생각해요.", "infer"),
            new QA("잎싹이 초록머리를 지키려 한 마음은 어땠을까요?", "무섭고 힘들어도 꼭 지켜 주고 싶은 엄마의 마음이었을 것 같아요.", "feel"),
            new QA("나라면 잎싹처럼 위험을 무릅쓰고 원하는 일을 할 수 있을까요?", "저도 하고 싶은 일이 있으면 겁이 나도 한번은 도전해 보고 싶어요.", "connect")),
        "푸른 사자 와니니", List.of(
            new QA("와니니가 무리에서 쫓겨난 까닭은 무엇인가요?", "사냥에 자꾸 실패해서 무리를 이끄는 어른 사자들에게 쫓겨났어요.", "find"),
            new QA("와니니가 무리를 떠난 뒤에도 계속 앞으로 나아간 까닭은 무엇일까요?", "두려워도 자기 힘으로 살아갈 길을 찾고 싶었기 때문이에요.", "infer"),
            new QA("와니니가 무리에서 쫓겨났을 때 어떤 마음이 들었나요?", "혼자 남겨진 와니니가 안쓰럽고, 저도 친구들에게 따돌림당하면 저렇게 슬플 것 같다고 느꼈어요.", "feel"),
            new QA("나라면 친구와 떨어졌을 때 어떻게 했을까요?", "혼자 겁내기보다 주변을 살피고 믿을 수 있는 친구를 찾아 도움을 부탁했을 거예요.", "connect")),
        "세계를 바꾸는 착한 기술", List.of(
            new QA("착한 기술은 누구를 돕기 위해 만들어지나요?", "생활에 어려움을 겪는 사람들과 환경을 돕기 위해 만들어져요.", "find"),
            new QA("이런 기술들이 '착한 기술'이라고 불리는 까닭은 무엇일까요?", "돈을 많이 벌기 위한 기술이 아니라 어려운 사람들의 생활을 편하게 도와주는 기술이기 때문이라고 생각해요.", "infer"),
            new QA("착한 기술 이야기를 읽으면서 어떤 마음이 들었나요?", "간단한 물건으로 사람들을 도울 수 있다는 것이 놀랍고, 저도 그런 아이디어를 내 보고 싶다는 마음이 들었어요.", "feel"),
            new QA("내가 생활 속에서 실천해 보고 싶은 착한 행동은 무엇인가요?", "물을 아끼고 사용하지 않는 전등을 바로 끄고 싶어요.", "connect"))
    );
    private static final List<Example> EXAMPLES = List.of(
        new Example("ss01", "김초롱", "마당을 나온 암탉", "황선미", "story", 5,
            "잎싹이 자기 삶을 스스로 선택하는 모습이 용기 있게 느껴졌어요.",
            "잎싹이 알을 품기 위해 마당을 나온 뒤 어려움을 이겨 내고 초록머리를 지키는 이야기예요.",
            "잎싹의 선택", "안전한 마당에 남는다", "꿈을 위해 마당 밖으로 나간다",
            "잎싹은 왜 그렇게 알을 품고 싶어 했을까요?", 6),
        new Example("demo_student_02", "송민정", "긴긴밤", "루리", "story", 5,
            "서로 다른 존재가 친구가 되어 끝까지 돕는 장면이 오래 기억에 남아요.",
            "노든은 힘든 일을 겪고 혼자가 되지만 어린 펭귄과 함께 긴 여행을 시작합니다. 둘은 위험한 순간을 함께 이겨 내며 서로 의지하는 가족 같은 사이가 됩니다. 긴 여행 끝에 펭귄은 노든의 도움을 받아 자신의 길을 찾아 나아갑니다.",
            "노든의 약속", "혼자 안전한 곳으로 간다", "어린 펭귄과 함께 바다로 간다",
            "노든과 어린 펭귄은 어떻게 서로의 가족이 되었을까요?", 5),
        new Example("demo_student_03", "박하민", "아낌없이 주는 나무", "셸 실버스타인", "story", 4,
            "나무가 소년에게 계속 나누어 주는 것을 보고 진짜 사랑이 무엇인지 생각했어요.",
            "나무는 어린 소년을 좋아해 그가 원하는 것을 아낌없이 내어 줍니다. 소년이 자라는 동안 사과와 가지, 줄기까지 주면서 계속 도움을 줍니다. 시간이 흘러 늙은 소년이 돌아오자 나무는 남은 밑동까지 쉴 곳으로 내어 줍니다.",
            "나무의 마지막 선물", "남은 그루터기를 지킨다", "소년에게 쉴 자리를 내어 준다",
            "나무는 왜 소년에게 계속 자신의 것을 내어 주었을까요?", 8),
        new Example("demo_student_04", "이진우", "수상한 도서관", "박현숙", "story", 3,
            "도서관에서 비밀을 찾는 부분이 재미있었어요.",
            "주인공은 평범해 보이던 도서관에서 이상한 일들을 발견하게 됩니다. 친구들과 단서를 찾으며 도서관에 숨겨진 비밀을 하나씩 풀어 갑니다. 그 과정에서 서로 힘을 합치고 문제를 해결하는 경험을 하게 됩니다.",
            "비밀을 발견했다면", "혼자 확인한다", "친구들에게 바로 알린다",
            "도서관에서 어떤 수상한 일이 벌어질지 궁금하지 않나요?", 6),
        new Example("demo_student_05", "김민지", "우리 몸의 신비", "김은영", "info", 3,
            "심장이 쉬지 않고 움직인다는 사실이 신기했어요.",
            "우리 몸에는 심장, 뇌, 폐처럼 서로 다른 일을 하는 여러 기관이 있습니다. 각 기관은 따로 움직이는 것이 아니라 서로 도우며 우리 몸을 건강하게 유지합니다. 이 책은 우리 몸의 구조와 기능을 알기 쉽게 설명해 줍니다.",
            "건강 습관 하나", "늦게까지 깨어 있는다", "정해진 시간에 충분히 잔다",
            "우리 몸에서 가장 신기하다고 느낀 기관은 무엇인가요?", 3),
        new Example("demo_student_06", "서희원", "강아지똥", "권정생", "story", 5,
            "쓸모없다고 생각한 강아지똥이 꽃을 피우는 데 꼭 필요한 존재가 된 점이 감동적이었어요.",
            "아무도 필요 없다고 생각했던 강아지똥은 자신의 모습 때문에 슬퍼합니다. 어느 날 민들레가 꽃을 피우기 위해 강아지똥의 도움이 필요하다고 말합니다. 강아지똥은 자신도 누군가에게 소중한 존재가 될 수 있다는 것을 알게 됩니다.",
            "강아지똥의 선택", "그 자리에 그대로 남는다", "민들레가 피도록 자신을 내어 준다",
            "강아지똥은 자신이 쓸모없다고 생각하다가 무엇을 깨닫게 되었을까요?", 4),
        new Example("demo_student_07", "김수진", "책 먹는 여우", "프란치스카 비어만", "story", 4,
            "책을 너무 좋아하는 여우의 모습이 재미있고 나도 더 많은 책을 읽고 싶어졌어요.",
            "여우는 책을 너무 좋아해서 읽은 뒤에는 소금과 후추를 뿌려 책까지 먹어 버립니다. 책을 구하지 못하게 되자 어려움을 겪지만, 결국 자신이 직접 이야기를 쓰기 시작합니다. 여우는 자신이 만든 책으로 새로운 즐거움을 찾게 됩니다.",
            "새 책을 발견하면", "바로 맛부터 본다", "먼저 끝까지 읽어 본다",
            "여우는 왜 그렇게 책을 좋아하게 되었을까요?", 7),
        new Example("demo_student_08", "이혜원", "초정리 편지", "배유안", "story", 4,
            "장운이가 글을 배우면서 생각을 전하는 과정이 좋았어요.",
            "장운이는 글을 배우면서 자신의 생각과 마음을 편지에 담아 전하게 됩니다. 여러 사람과 편지를 주고받으며 새로운 세상을 알아가고 점점 성장합니다. 글을 배우는 것이 사람의 생각과 삶을 바꿀 수 있다는 내용입니다.",
            "글을 배울 기회", "일이 바빠 포기한다", "조금씩이라도 꾸준히 배운다",
            "주인공이 편지를 통해 가장 크게 달라진 점은 무엇일까요?", 5)
    );

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReadingRecordRepository readingRecordRepository;
    private final ResponseRepository responseRepository;
    private final SummaryRepository summaryRepository;
    private final BookRecommendationRepository recommendationRepository;
    private final ContentLikeRepository contentLikeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (int index = 0; index < EXAMPLES.size(); index++) {
            Example example = EXAMPLES.get(index);
            User student = userRepository.findByLoginId(example.loginId()).orElse(null);
            if (student == null || !Boolean.TRUE.equals(student.getDemoAccount())) continue;
            ReadingRecord record = findOrCreateCompletedRecord(student, example, index);
            List<Response> questions = ensureAfterQuestions(student, record, example);
            ensureBeforeQuestions(student, record, example.bookTitle());
            ensureDuringQuestions(student, record, example.bookTitle());
            ensureSummary(student, record, example);
            BookChatSeed chatSeed = BOOK_CHAT_SEEDS.get(index);
            Response chatPost = ensureBookChatPost(student, record, example, chatSeed);
            ensureBookChatReplies(chatPost, chatSeed);
            ensureRecommendation(student, record, example, questions);
        }

        /* 대표 학생 보관함에는 서로 다른 달의 완독 기록이 여러 권 보이게 한다. */
        User representative = userRepository.findByLoginId("ss01").orElse(null);
        if (representative != null && Boolean.TRUE.equals(representative.getDemoAccount())) {
            ensureExtraArchiveBook(representative, "푸른 사자 와니니", "이현", "story", 4, 2,
                "와니니는 무리를 떠난 뒤 어떻게 혼자 살아가는 법을 배웠을까요?", 4,
                "와니니는 무리를 이끄는 데 서투르다는 이유로 초원의 사자 무리에서 쫓겨납니다. 혼자가 된 와니니는 무섭고 힘든 순간들을 이겨 내며 스스로 사냥하는 법을 배워 갑니다. 그러다 다른 이유로 무리를 떠난 사자들을 만나 서로 의지하며 새로운 무리를 이루게 됩니다. 힘든 시간을 겪으며 와니니는 처음보다 훨씬 강하고 지혜로운 사자로 자라납니다.");
            ensureExtraArchiveBook(representative, "세계를 바꾸는 착한 기술", "김정희", "info", 5, 4,
                "책에 나온 기술 중 내가 가장 써 보고 싶은 것은 무엇인가요?", 3,
                "세계에는 깨끗한 물이나 전기가 부족해 불편하게 생활하는 사람들이 있습니다. 이 책은 이런 사람들을 돕기 위한 여러 가지 착한 기술을 소개합니다. 간단한 기술로 사람들의 생활을 편리하게 하고 환경도 지킬 수 있다는 것을 알려 줍니다.");
        }
    }

    private ReadingRecord findOrCreateCompletedRecord(User student, Example example, int index) {
        Book book = bookRepository.findFirstBySourceAndTitleAndAuthor(
            "individual", example.bookTitle(), example.author()).orElseGet(() -> {
                Book created = new Book();
                created.setTitle(example.bookTitle());
                created.setAuthor(example.author());
                created.setBookType(example.bookType());
                created.setDescription("심사 체험용 완독 예시");
                created.setSource("individual");
                created.setRegisteredBy(student);
                return bookRepository.save(created);
            });

        ReadingRecord existing = readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(student.getId())
            .stream().filter(record -> record.getBook() != null && example.bookTitle().equals(record.getBook().getTitle()))
            .findFirst().orElse(null);
        if (existing != null) return existing;

        ReadingRecord record = new ReadingRecord();
        record.setStudent(student);
        record.setBook(book);
        record.setCurrentStage("completed");
        record.setBeforeDone(true);
        record.setDuringDone(true);
        record.setAfterDone(true);
        record.setCurrentPage(120 + index * 8);
        record.setTotalPages(120 + index * 8);
        record.setRating(example.rating());
        record.setFinalReadingPracticeScore(90 - index * 5);
        record.setFinalRecordCompletionScore(94 - index * 6);
        record.setFinishedAt(LocalDate.now().minusMonths(index % 5).atTime(15, 20));
        return readingRecordRepository.save(record);
    }

    private List<Response> ensureAfterQuestions(User student, ReadingRecord record, Example example) {
        List<Response> existing = responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                student.getId(), record.getId(), "individual", "answer", "after");
        List<Response> demoQuestions = existing.stream().filter(this::isDemoSeed).toList();

        List<String> questions = List.of(
            example.teaserQuestion(),
            "주인공의 마음은 어떻게 달라졌나요?",
            "이 책을 읽고 새롭게 생각한 점은 무엇인가요?");
        List<String> answers = List.of(
            example.summary(),
            example.reason(),
            "나도 어려운 일이 있어도 친구와 함께 끝까지 해 보고 싶습니다.");

        /*
         * 첫 번째 질문(questionIndex=1)은 "우리 반 추천 책장"의 "이 책이
         * 궁금해지는 질문" 미리보기로도 재사용된다(ensureRecommendation
         * 참고). 예전 고정 문구로 이미 저장된 행이 있으면 새 행을 만들지
         * 않고 텍스트만 책마다 다른 값으로 보정한다.
         */
        if (!demoQuestions.isEmpty()) {
            Response first = demoQuestions.get(0);
            Object currentQuestion = first.getExtraData() == null ? null : first.getExtraData().get("question");
            if (!questions.get(0).equals(currentQuestion)) {
                Map<String, Object> updatedExtra = new HashMap<>(first.getExtraData());
                updatedExtra.put("question", questions.get(0));
                first.setExtraData(updatedExtra);
                first.setContent(answers.get(0));
                responseRepository.save(first);
            }
        }

        if (demoQuestions.size() >= 3) return demoQuestions;

        List<Response> saved = new ArrayList<>(demoQuestions);
        for (int i = demoQuestions.size(); i < 3; i++) {
            Response response = new Response();
            response.setStudent(student);
            response.setBook(record.getBook());
            response.setReadingRecord(record);
            response.setMode("individual");
            response.setContentType("answer");
            response.setStage("after");
            response.setActivityDate(LocalDate.now());
            response.setContent(answers.get(i));
            response.setPassed(true);
            response.setStatus("approved");
            response.setExtraData(Map.of(
                "demoSeed", DEMO_MARKER,
                "questionIndex", i + 1,
                "question", questions.get(i),
                "answer", answers.get(i)));
            saved.add(responseRepository.save(response));
        }
        return saved;
    }

    /*
     * "책 추천하기 -> 질문 선택하기" 화면(BookRecommendationService.
     * getCompletedBookQuestions)이 읽기 전 후보도 여러 개 보여줄 수 있도록
     * BEFORE_QUESTIONS에 등록된 책에 한해 stage="before" Response 행을
     * 멱등 생성한다. 등록되지 않은 책은 건드리지 않는다(빈 목록 반환).
     */
    private void ensureBeforeQuestions(User student, ReadingRecord record, String bookTitle) {
        List<QA> seeds = BEFORE_QUESTIONS.get(bookTitle);
        if (seeds == null || seeds.isEmpty()) return;

        List<Response> existing = responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                student.getId(), record.getId(), "individual", "answer", "before");
        List<Response> demoQuestions = existing.stream().filter(this::isDemoSeed).toList();
        if (demoQuestions.size() >= seeds.size()) return;

        for (int i = demoQuestions.size(); i < seeds.size(); i++) {
            QA qa = seeds.get(i);
            Response response = new Response();
            response.setStudent(student);
            response.setBook(record.getBook());
            response.setReadingRecord(record);
            response.setMode("individual");
            response.setContentType("answer");
            response.setStage("before");
            response.setActivityDate(LocalDate.now());
            response.setContent(qa.answer());
            response.setPassed(true);
            response.setStatus("approved");
            response.setExtraData(Map.of(
                "demoSeed", DEMO_MARKER,
                "stepType", qa.type(),
                "question", qa.question(),
                "answer", qa.answer()));
            responseRepository.save(response);
        }
    }

    /* ensureBeforeQuestions와 같은 방식으로 stage="during" 행을 채운다. */
    private void ensureDuringQuestions(User student, ReadingRecord record, String bookTitle) {
        List<QA> seeds = DURING_QUESTIONS.get(bookTitle);
        if (seeds == null || seeds.isEmpty()) return;

        List<Response> existing = responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                student.getId(), record.getId(), "individual", "answer", "during");
        List<Response> demoQuestions = existing.stream().filter(this::isDemoSeed).toList();
        if (demoQuestions.size() >= seeds.size()) return;

        for (int i = demoQuestions.size(); i < seeds.size(); i++) {
            QA qa = seeds.get(i);
            Response response = new Response();
            response.setStudent(student);
            response.setBook(record.getBook());
            response.setReadingRecord(record);
            response.setMode("individual");
            response.setContentType("answer");
            response.setStage("during");
            response.setActivityDate(LocalDate.now());
            response.setContent(qa.answer());
            response.setPassed(true);
            response.setStatus("approved");
            response.setExtraData(Map.of(
                "demoSeed", DEMO_MARKER,
                "questionType", qa.type(),
                "question", qa.question(),
                "answer", qa.answer()));
            responseRepository.save(response);
        }
    }

    private void ensureSummary(User student, ReadingRecord record, Example example) {
        Summary summary = summaryRepository.findByStudent_IdAndReadingRecord_Id(student.getId(), record.getId())
            .orElse(null);
        if (summary != null) {
            /* 기존 행을 재사용하고 문장 내용만 최신 값으로 보정한다(새 행 생성 없음). */
            if (!example.summary().equals(summary.getSummaryText())) {
                summary.setSummaryText(example.summary());
                summaryRepository.save(summary);
            }
            return;
        }
        summary = new Summary();
        summary.setStudent(student);
        summary.setBook(record.getBook());
        summary.setReadingRecord(record);
        summary.setBookType(example.bookType());
        summary.setSummaryText(example.summary());
        summary.setIsShared(true);
        summary.setStatus("approved");
        summary.setAiPassed(true);
        summaryRepository.save(summary);
    }

    private Response ensureBookChatPost(
            User student, ReadingRecord record, Example example, BookChatSeed chatSeed) {
        Response existing = responseRepository.findByStudent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdDesc(
            List.of(student.getId()), "individual", "chat_post").stream().filter(this::isDemoSeed)
            .findFirst().orElse(null);
        User demoTeacher = userRepository.findByLoginId("tt11").orElse(null);
        Response post = existing == null ? new Response() : existing;
        post.setStudent(student);
        post.setReadingRecord(record);
        post.setBook(record.getBook());
        post.setMode("individual");
        post.setContentType("chat_post");
        post.setContent(chatSeed.scene());
        if (post.getActivityDate() == null) post.setActivityDate(LocalDate.now());
        post.setStatus("approved");
        post.setReviewedBy(demoTeacher);
        post.setReviewedAt(LocalDateTime.now());
        Map<String, Object> data = new HashMap<>();
        data.put("demoSeed", DEMO_MARKER);
        data.put("bookTitle", example.bookTitle());
        data.put("title", chatSeed.title());
        data.put("optionA", chatSeed.optionA());
        data.put("optionB", chatSeed.optionB());
        data.put("authorChoice", chatSeed.authorChoice());
        post.setExtraData(data);
        return responseRepository.save(post);
    }

    private void ensureBookChatReplies(Response post, BookChatSeed chatSeed) {
        if (post == null) return;
        List<Response> existing = responseRepository
            .findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                post.getId(), "individual", "chat_reply").stream()
            .filter(this::isDemoSeed)
            .toList();

        for (int i = 0; i < chatSeed.replies().size(); i++) {
            BookChatReplySeed source = chatSeed.replies().get(i);
            User writer = userRepository.findByLoginId(source.loginId()).orElse(null);
            if (writer == null || !Boolean.TRUE.equals(writer.getDemoAccount())) continue;
            Response reply = i < existing.size() ? existing.get(i) : new Response();
            reply.setStudent(writer);
            reply.setParent(post);
            reply.setMode("individual");
            reply.setContentType("chat_reply");
            reply.setContent(source.content());
            reply.setStatus("approved");
            if (reply.getActivityDate() == null) reply.setActivityDate(LocalDate.now());
            reply.setExtraData(Map.of("demoSeed", DEMO_MARKER, "choice", source.choice()));
            responseRepository.save(reply);
        }
    }

    /*
     * "우리 반 추천 책장" 카드를 멱등 생성한다. 학생+제목으로 기존 행을
     * 찾으므로(예전에 reason 앞에 "[심사 체험 예시] " 표기가 붙어 있었을 때도
     * 같은 행을 찾아낸다) 재시작할 때마다 새 행이 쌓이지 않는다. 예전 표기가
     * 남아 있으면 지우고, 책마다 다른 초기 좋아요 수를 맞춰 둔다.
     */
    private void ensureRecommendation(User student, ReadingRecord record, Example example, List<Response> questions) {
        BookRecommendation recommendation = recommendationRepository
            .findByStudent_IdOrderByCreatedAtDescIdDesc(student.getId()).stream()
            .filter(item -> example.bookTitle().equals(item.getTitle()))
            .findFirst()
            .orElse(null);

        if (recommendation == null) {
            recommendation = new BookRecommendation();
            recommendation.setStudent(student);
            recommendation.setReadingRecord(record);
            recommendation.setTitle(example.bookTitle());
            recommendation.setAuthor(example.author());
            recommendation.setReason(example.reason());
            recommendation.setTeaserResponseIds(questions.isEmpty() ? List.of() : List.of(questions.get(0).getId()));
            recommendation = recommendationRepository.save(recommendation);
        } else if (recommendation.getReason() != null && recommendation.getReason().startsWith(LEGACY_REASON_PREFIX)) {
            recommendation.setReason(recommendation.getReason().substring(LEGACY_REASON_PREFIX.length()));
            recommendation = recommendationRepository.save(recommendation);
        }

        ensureRecommendationLikes(recommendation, student, example.likeCount());
    }

    /*
     * 추천 카드마다 서로 다른 "초기 좋아요 수"를 실제 ContentLike 행으로
     * 채운다. 본인 좋아요는 세지 않고(작성자 제외), 심사 학생 8명 중 아직
     * 좋아요를 누르지 않은 학생을 순서대로 채워 넣는다 - 학생 수가 한정돼
     * 있어 목표 개수가 (전체 8명 - 본인)을 넘으면 그 한도까지만 채워진다.
     * 이미 채워진 행은 다시 만들지 않는다(멱등). 심사위원이 화면에서 직접
     * 누르는 좋아요는 여기서 건드리지 않고 프론트 sessionStorage에서만
     * 별도로 더해 보여준다(공용 DB 좋아요 값은 영구 변경되지 않는다).
     */
    private void ensureRecommendationLikes(BookRecommendation recommendation, User author, int targetLikeCount) {
        for (String loginId : DEMO_STUDENT_LOGIN_IDS) {
            long alreadyLiked = contentLikeRepository.countByContentTypeAndContentId(
                CONTENT_TYPE_BOOK_RECOMMENDATION, recommendation.getId());
            if (alreadyLiked >= targetLikeCount) return;
            if (loginId.equals(author.getLoginId())) continue;

            User liker = userRepository.findByLoginId(loginId)
                .filter(user -> Boolean.TRUE.equals(user.getDemoAccount()))
                .orElse(null);
            if (liker == null) continue;

            if (contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(
                    liker.getId(), CONTENT_TYPE_BOOK_RECOMMENDATION, recommendation.getId()).isPresent()) {
                continue;
            }

            ContentLike like = new ContentLike();
            like.setStudent(liker);
            like.setContentType(CONTENT_TYPE_BOOK_RECOMMENDATION);
            like.setContentId(recommendation.getId());
            contentLikeRepository.save(like);
        }
    }

    private void ensureExtraArchiveBook(User student, String title, String author, String type, int rating,
            int monthsAgo, String teaserQuestion, int likeCount, String summary) {
        Example extra = new Example(student.getLoginId(), student.getName(), title, author, type, rating,
            "새로운 내용을 알게 되어 친구에게도 추천하고 싶어요.",
            summary,
            "내가 주인공이라면", "혼자 해결한다", "친구와 방법을 찾는다",
            teaserQuestion, likeCount);
        ReadingRecord record = findOrCreateCompletedRecord(student, extra, monthsAgo);
        record.setFinishedAt(LocalDate.now().minusMonths(monthsAgo).atTime(14, 10));
        readingRecordRepository.save(record);
        List<Response> questions = ensureAfterQuestions(student, record, extra);
        ensureBeforeQuestions(student, record, title);
        ensureDuringQuestions(student, record, title);
        ensureSummary(student, record, extra);
        ensureRecommendation(student, record, extra, questions);
    }

    private boolean isDemoSeed(Response response) {
        return response.getExtraData() != null && DEMO_MARKER.equals(response.getExtraData().get("demoSeed"));
    }

    private record Example(String loginId, String studentName, String bookTitle, String author,
        String bookType, int rating, String reason, String summary, String chatTitle,
        String optionA, String optionB, String teaserQuestion, int likeCount) {}

    private record BookChatSeed(String loginId, String title, String scene,
        String optionA, String optionB, String authorChoice, List<BookChatReplySeed> replies) {}

    private record BookChatReplySeed(String loginId, String choice, String content) {}

    /* type은 읽기 전이면 stepType, 읽는 중이면 questionType 값이다. */
    private record QA(String question, String answer, String type) {}
}
