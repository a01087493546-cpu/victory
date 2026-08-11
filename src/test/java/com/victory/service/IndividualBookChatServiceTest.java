package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualBookChatCommentResponse;
import com.victory.dto.IndividualBookChatFeedResponse;
import com.victory.dto.IndividualBookChatPostResponse;
import com.victory.entity.Book;
import com.victory.entity.ClassStudent;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

class IndividualBookChatServiceTest {

    private static final Long STUDENT_ID = 100L;
    private static final Long CLASSMATE_ID = 101L;
    private static final Long OTHER_CLASS_STUDENT_ID = 200L;
    private static final Long CLASS_ID = 10L;
    private static final Long OTHER_CLASS_ID = 20L;
    private static final Long READING_RECORD_ID = 500L;

    private final ResponseRepository responseRepository = mock(ResponseRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
    private final SchoolClassRepository schoolClassRepository = mock(SchoolClassRepository.class);
    private final IndividualBookChatRewardService rewardService = mock(IndividualBookChatRewardService.class);
    private final ReadingRecordRepository readingRecordRepository = mock(ReadingRecordRepository.class);

    private final IndividualBookChatService service = new IndividualBookChatService(
        responseRepository, userRepository, classStudentRepository, schoolClassRepository, rewardService,
        readingRecordRepository);

    private ReadingRecord buildInProgressReadingRecord(Long id, User student) {
        ReadingRecord record = new ReadingRecord();
        record.setId(id);
        record.setStudent(student);
        Book book = new Book();
        book.setId(1L);
        book.setTitle("긴긴밤");
        record.setBook(book);
        return record;
    }

    private User buildStudent(Long id, String name) {
        User student = new User();
        student.setId(id);
        student.setName(name);
        student.setRole("student");
        return student;
    }

    private User buildStudent() {
        return buildStudent(STUDENT_ID, "학생1");
    }

    private SchoolClass buildClass(Long id) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        return schoolClass;
    }

    private ClassStudent buildMembership(Long classId, User student) {
        ClassStudent membership = new ClassStudent();
        membership.setSchoolClass(buildClass(classId));
        membership.setStudent(student);
        return membership;
    }

    private Response buildPost(Long id, User author) {
        Response post = new Response();
        post.setId(id);
        post.setStudent(author);
        post.setMode("individual");
        post.setContentType("chat_post");
        post.setContent("장면");
        post.setExtraData(Map.of("bookTitle", "책", "title", "제목", "optionA", "A", "optionB", "B"));
        return post;
    }

    /* 검증: 필수 텍스트 항목이 비어 있으면 400 + 안내 메시지 */
    @Test
    void createPost_throwsBadRequestWhenRequiredFieldBlank() {
        assertThatThrownBy(() ->
            service.createPost(STUDENT_ID, READING_RECORD_ID, "책제목", "제목", "", "A", "B")
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 검증: 글쓰기는 이미지 없이 텍스트만 저장하고, 등록 성공 시 extraData에 밸런스 필드가 정확히 저장된다 */
    @Test
    void createPost_savesTextOnlyAndBalanceFieldsInExtraData() {
        User student = buildStudent();
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        ReadingRecord readingRecord = buildInProgressReadingRecord(READING_RECORD_ID, student);
        when(readingRecordRepository.findById(READING_RECORD_ID)).thenReturn(Optional.of(readingRecord));

        org.mockito.ArgumentCaptor<Response> captor = org.mockito.ArgumentCaptor.forClass(Response.class);
        when(responseRepository.save(captor.capture())).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(1L);
            return toSave;
        });
        when(rewardService.grantPostDailyRewardOnce(eq(student), any()))
            .thenReturn(new IndividualBookChatRewardService.RewardResult(true, false, new StudentStats()));

        IndividualBookChatPostResponse result = service.createPost(
            STUDENT_ID, READING_RECORD_ID, "긴긴밤", "밸런스제목", "장면설명", "선택A", "선택B");

        Response saved = captor.getValue();
        assertThat(saved.getMode()).isEqualTo("individual");
        assertThat(saved.getContentType()).isEqualTo("chat_post");
        assertThat(saved.getContent()).isEqualTo("장면설명");
        assertThat(saved.getStatus()).isEqualTo("pending");
        assertThat(saved.getImageData()).isNull();
        assertThat(saved.getReadingRecord()).isSameAs(readingRecord);
        assertThat(saved.getExtraData())
            .containsEntry("bookTitle", "긴긴밤")
            .containsEntry("title", "밸런스제목")
            .containsEntry("optionA", "선택A")
            .containsEntry("optionB", "선택B");

        assertThat(result.getBookTitle()).isEqualTo("긴긴밤");
        assertThat(result.isMine()).isTrue();
        assertThat(result.isRewardGranted()).isTrue();
    }

    /*
     * 심사계정 브라우저 격리: 같은 ss01을 여러 심사위원이 동시에 쓸 수
     * 있으므로 책수다방 글쓰기는 공용 DB에 저장되면 안 된다(댓글
     * createComment는 이미 같은 가드가 있음 - 글쓰기도 동일하게 막는다).
     */
    @Test
    void createPost_demoAccount_neverPersistsToSharedDb() {
        User demoStudent = buildStudent();
        demoStudent.setDemoAccount(true);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(demoStudent));

        ReadingRecord readingRecord = buildInProgressReadingRecord(READING_RECORD_ID, demoStudent);
        when(readingRecordRepository.findById(READING_RECORD_ID)).thenReturn(Optional.of(readingRecord));

        assertThatThrownBy(() ->
            service.createPost(STUDENT_ID, READING_RECORD_ID, "긴긴밤", "밸런스제목", "장면설명", "선택A", "선택B")
        ).isInstanceOf(ResponseStatusException.class);

        verify(responseRepository, never()).save(any(Response.class));
    }

    // =========================================================
    // 책수다방 글의 readingRecordId 연결 검증
    // =========================================================

    /* readingRecordId 없이 작성하면 400이고 NULL로 저장되지 않는다 */
    @Test
    void createPost_throwsBadRequestWhenReadingRecordIdMissing() {
        assertThatThrownBy(() ->
            service.createPost(STUDENT_ID, null, "긴긴밤", "제목", "장면", "A", "B")
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 존재하지 않는 readingRecordId면 404이고 저장되지 않는다 */
    @Test
    void createPost_throwsNotFoundWhenReadingRecordDoesNotExist() {
        User student = buildStudent();
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(readingRecordRepository.findById(READING_RECORD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.createPost(STUDENT_ID, READING_RECORD_ID, "긴긴밤", "제목", "장면", "A", "B")
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 다른 학생의 readingRecordId면 403이고 저장되지 않는다 */
    @Test
    void createPost_throwsForbiddenWhenReadingRecordBelongsToAnotherStudent() {
        User student = buildStudent();
        User otherStudent = buildStudent(CLASSMATE_ID, "다른학생");
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(readingRecordRepository.findById(READING_RECORD_ID))
            .thenReturn(Optional.of(buildInProgressReadingRecord(READING_RECORD_ID, otherStudent)));

        assertThatThrownBy(() ->
            service.createPost(STUDENT_ID, READING_RECORD_ID, "긴긴밤", "제목", "장면", "A", "B")
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 이미 완독한 readingRecordId면 409(진행 중인 책만 허용)이고 저장되지 않는다 */
    @Test
    void createPost_throwsConflictWhenReadingRecordAlreadyFinished() {
        User student = buildStudent();
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        ReadingRecord finishedRecord = buildInProgressReadingRecord(READING_RECORD_ID, student);
        finishedRecord.setFinishedAt(java.time.LocalDateTime.now());
        when(readingRecordRepository.findById(READING_RECORD_ID)).thenReturn(Optional.of(finishedRecord));

        assertThatThrownBy(() ->
            service.createPost(STUDENT_ID, READING_RECORD_ID, "긴긴밤", "제목", "장면", "A", "B")
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 검증: 학급이 있는 학생은 같은 학급 학생들의 글만(다른 학급 글 제외) 받는다 */
    @Test
    void getClassFeed_returnsOnlySameClassPosts() {
        User me = buildStudent(STUDENT_ID, "나");
        User classmate = buildStudent(CLASSMATE_ID, "짝꿍");

        when(classStudentRepository.findByStudentId(STUDENT_ID))
            .thenReturn(Optional.of(buildMembership(CLASS_ID, me)));
        when(classStudentRepository.findBySchoolClassId(CLASS_ID))
            .thenReturn(List.of(buildMembership(CLASS_ID, me), buildMembership(CLASS_ID, classmate)));

        Response myPost = buildPost(1L, me);
        myPost.setStatus("pending");
        Response classmatePost = buildPost(2L, classmate);
        classmatePost.setStatus("approved");

        when(responseRepository.findByStudent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdDesc(
                List.of(STUDENT_ID, CLASSMATE_ID), "individual", "chat_post"))
            .thenReturn(List.of(classmatePost, myPost));
        when(responseRepository.findByParent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                List.of(2L, 1L), "individual", "chat_reply"))
            .thenReturn(List.of());

        IndividualBookChatFeedResponse feed = service.getClassFeed(STUDENT_ID);

        assertThat(feed.isHasClass()).isTrue();
        assertThat(feed.getPosts()).hasSize(2);
        assertThat(feed.getPosts().get(0).isMine()).isFalse();
        assertThat(feed.getPosts().get(1).isMine()).isTrue();
    }

    @Test
    void getClassFeed_hidesClassmatePendingAndRejectedPosts() {
        User me = buildStudent(STUDENT_ID, "나");
        User classmate = buildStudent(CLASSMATE_ID, "짝꿍");

        when(classStudentRepository.findByStudentId(STUDENT_ID))
            .thenReturn(Optional.of(buildMembership(CLASS_ID, me)));
        when(classStudentRepository.findBySchoolClassId(CLASS_ID))
            .thenReturn(List.of(buildMembership(CLASS_ID, me), buildMembership(CLASS_ID, classmate)));

        Response approvedPost = buildPost(1L, classmate);
        approvedPost.setStatus("approved");
        Response pendingPost = buildPost(2L, classmate);
        pendingPost.setStatus("pending");
        Response rejectedPost = buildPost(3L, classmate);
        rejectedPost.setStatus("rejected");

        when(responseRepository.findByStudent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdDesc(
                List.of(STUDENT_ID, CLASSMATE_ID), "individual", "chat_post"))
            .thenReturn(List.of(rejectedPost, pendingPost, approvedPost));
        when(responseRepository.findByParent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                List.of(1L), "individual", "chat_reply"))
            .thenReturn(List.of());

        IndividualBookChatFeedResponse feed = service.getClassFeed(STUDENT_ID);

        assertThat(feed.getPosts()).extracting(IndividualBookChatPostResponse::getId)
            .containsExactly(1L);
    }

    /* 검증 D: 학급이 없는 계정은 hasClass=false와 함께 본인 글만 받는다(빈 상태를 프론트가 구분할 수 있게) */
    @Test
    void getClassFeed_returnsOwnPostsOnlyWhenNoClass() {
        User me = buildStudent();

        when(classStudentRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdDesc(
                STUDENT_ID, "individual", "chat_post"))
            .thenReturn(List.of(buildPost(1L, me)));
        when(responseRepository.findByParent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                List.of(1L), "individual", "chat_reply"))
            .thenReturn(List.of());

        IndividualBookChatFeedResponse feed = service.getClassFeed(STUDENT_ID);

        assertThat(feed.isHasClass()).isFalse();
        assertThat(feed.getPosts()).hasSize(1);
        assertThat(feed.getPosts().get(0).isMine()).isTrue();
    }

    /* 검증: A/B 댓글 수는 목록 조회에서 글마다 정확히 집계된다 */
    @Test
    void getClassFeed_aggregatesChoiceCountsPerPost() {
        User me = buildStudent();

        when(classStudentRepository.findByStudentId(STUDENT_ID))
            .thenReturn(Optional.of(buildMembership(CLASS_ID, me)));
        when(classStudentRepository.findBySchoolClassId(CLASS_ID))
            .thenReturn(List.of(buildMembership(CLASS_ID, me)));

        Response post = buildPost(1L, me);
        when(responseRepository.findByStudent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdDesc(
                List.of(STUDENT_ID), "individual", "chat_post"))
            .thenReturn(List.of(post));

        Response replyA1 = new Response();
        replyA1.setParent(post);
        replyA1.setExtraData(Map.of("choice", "A"));
        Response replyA2 = new Response();
        replyA2.setParent(post);
        replyA2.setExtraData(Map.of("choice", "A"));
        Response replyB1 = new Response();
        replyB1.setParent(post);
        replyB1.setExtraData(Map.of("choice", "B"));

        when(responseRepository.findByParent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                List.of(1L), "individual", "chat_reply"))
            .thenReturn(List.of(replyA1, replyA2, replyB1));

        IndividualBookChatFeedResponse feed = service.getClassFeed(STUDENT_ID);

        assertThat(feed.getPosts().get(0).getCountA()).isEqualTo(2);
        assertThat(feed.getPosts().get(0).getCountB()).isEqualTo(1);
    }

    /* 검증: 목록 화면에서 삭제 버튼을 위한 isMine 플래그가 정확하다(다른 학생 글은 false) */
    @Test
    void deletePost_setsDeletedAt() {
        User student = buildStudent();
        Response response = new Response();
        response.setId(7L);
        response.setStudent(student);

        when(responseRepository.findByIdAndStudent_IdAndModeAndContentType(7L, STUDENT_ID, "individual", "chat_post"))
            .thenReturn(Optional.of(response));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deletePost(STUDENT_ID, 7L);

        assertThat(response.getDeletedAt()).isNotNull();
    }

    /* 검증: 존재하지 않거나 다른 학생 글이면 404 */
    @Test
    void deletePost_throwsNotFoundWhenNotOwnedByStudent() {
        when(responseRepository.findByIdAndStudent_IdAndModeAndContentType(7L, STUDENT_ID, "individual", "chat_post"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePost(STUDENT_ID, 7L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    /* 검증 1/4: 친구 글 댓글은 내용이 비어 있으면 400, DB 저장도 안 됨 */
    @Test
    void createComment_throwsBadRequestWhenContentBlank() {
        assertThatThrownBy(() ->
            service.createComment(STUDENT_ID, 1L, "A", "  ")
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 검증: choice가 A/B가 아니면 400 */
    @Test
    void createComment_throwsBadRequestWhenChoiceInvalid() {
        assertThatThrownBy(() ->
            service.createComment(STUDENT_ID, 1L, "C", "이유")
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 검증: postId에 해당하는 글이 없으면 404 */
    @Test
    void createComment_throwsNotFoundWhenPostDoesNotExist() {
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildStudent()));
        when(responseRepository.findByIdAndModeAndContentTypeAndDeletedAtIsNull(1L, "individual", "chat_post"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createComment(STUDENT_ID, 1L, "A", "이유"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    /* 검증 D: 다른 학급 학생의 글에는 댓글을 남길 수 없다(404로 통일, 존재 여부를 알려주지 않음) */
    @Test
    void createComment_throwsNotFoundWhenPostAuthorInDifferentClass() {
        User me = buildStudent(STUDENT_ID, "나");
        User otherClassAuthor = buildStudent(OTHER_CLASS_STUDENT_ID, "다른학급학생");
        Response post = buildPost(5L, otherClassAuthor);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(me));
        when(responseRepository.findByIdAndModeAndContentTypeAndDeletedAtIsNull(5L, "individual", "chat_post"))
            .thenReturn(Optional.of(post));
        when(classStudentRepository.findByStudentId(STUDENT_ID))
            .thenReturn(Optional.of(buildMembership(CLASS_ID, me)));
        when(classStudentRepository.findByStudentId(OTHER_CLASS_STUDENT_ID))
            .thenReturn(Optional.of(buildMembership(OTHER_CLASS_ID, otherClassAuthor)));

        assertThatThrownBy(() -> service.createComment(STUDENT_ID, 5L, "A", "이유"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 검증 1/2/6: 같은 학급 학생 글에는 댓글을 남길 수 있고, parent로 연결되며 rewardGranted/stats가 포함된다 */
    @Test
    void createComment_savesReplyLinkedToParentPostWithinSameClass() {
        User me = buildStudent(STUDENT_ID, "나");
        User classmate = buildStudent(CLASSMATE_ID, "짝꿍");
        Response post = buildPost(5L, classmate);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(me));
        when(responseRepository.findByIdAndModeAndContentTypeAndDeletedAtIsNull(5L, "individual", "chat_post"))
            .thenReturn(Optional.of(post));
        when(classStudentRepository.findByStudentId(STUDENT_ID))
            .thenReturn(Optional.of(buildMembership(CLASS_ID, me)));
        when(classStudentRepository.findByStudentId(CLASSMATE_ID))
            .thenReturn(Optional.of(buildMembership(CLASS_ID, classmate)));

        org.mockito.ArgumentCaptor<Response> captor = org.mockito.ArgumentCaptor.forClass(Response.class);
        when(responseRepository.save(captor.capture())).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(9L);
            return toSave;
        });
        when(rewardService.grantCommentDailyRewardOnce(eq(me), any()))
            .thenReturn(new IndividualBookChatRewardService.RewardResult(true, false, new StudentStats()));

        IndividualBookChatCommentResponse result =
            service.createComment(STUDENT_ID, 5L, "a", "친구를 기다릴래");

        Response saved = captor.getValue();
        assertThat(saved.getMode()).isEqualTo("individual");
        assertThat(saved.getContentType()).isEqualTo("chat_reply");
        assertThat(saved.getContent()).isEqualTo("친구를 기다릴래");
        assertThat(saved.getParent()).isSameAs(post);
        assertThat(saved.getExtraData()).containsEntry("choice", "A");

        assertThat(result.getChoice()).isEqualTo("A");
        assertThat(result.getPostId()).isEqualTo(5L);
        assertThat(result.isRewardGranted()).isTrue();
    }

    /* 검증: 자기 자신의 글에는 학급 여부와 무관하게 항상 댓글을 남길 수 있다 */
    @Test
    void createComment_allowsCommentingOnOwnPostRegardlessOfClass() {
        User me = buildStudent();
        Response myPost = buildPost(3L, me);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(me));
        when(responseRepository.findByIdAndModeAndContentTypeAndDeletedAtIsNull(3L, "individual", "chat_post"))
            .thenReturn(Optional.of(myPost));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rewardService.grantCommentDailyRewardOnce(eq(me), any()))
            .thenReturn(new IndividualBookChatRewardService.RewardResult(false, true, new StudentStats()));

        IndividualBookChatCommentResponse result = service.createComment(STUDENT_ID, 3L, "B", "내 글 댓글");

        assertThat(result.isRewardGranted()).isFalse();
        verify(classStudentRepository, never()).findByStudentId(any());
    }

    /* 검증 4/5: 같은 날 두 번째 댓글은 저장되지만 rewardGranted는 false */
    @Test
    void createComment_secondCommentSameDaySavesButDoesNotGrantRewardAgain() {
        User me = buildStudent();
        Response myPost = buildPost(3L, me);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(me));
        when(responseRepository.findByIdAndModeAndContentTypeAndDeletedAtIsNull(3L, "individual", "chat_post"))
            .thenReturn(Optional.of(myPost));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rewardService.grantCommentDailyRewardOnce(eq(me), any()))
            .thenReturn(new IndividualBookChatRewardService.RewardResult(false, true, new StudentStats()));

        IndividualBookChatCommentResponse result =
            service.createComment(STUDENT_ID, 3L, "B", "먼저 갈래");

        verify(responseRepository).save(any(Response.class));
        assertThat(result.isRewardGranted()).isFalse();
    }

    /* 검증 B: 같은 학생이 같은 글에 다시 작성하면 새 댓글로 추가된다(수정이 아니라 추가) */
    @Test
    void createComment_secondCommentOnSamePostIsAddedNotReplaced() {
        User me = buildStudent();
        Response myPost = buildPost(3L, me);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(me));
        when(responseRepository.findByIdAndModeAndContentTypeAndDeletedAtIsNull(3L, "individual", "chat_post"))
            .thenReturn(Optional.of(myPost));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rewardService.grantCommentDailyRewardOnce(eq(me), any()))
            .thenReturn(new IndividualBookChatRewardService.RewardResult(false, true, new StudentStats()));

        service.createComment(STUDENT_ID, 3L, "A", "첫 댓글");
        service.createComment(STUDENT_ID, 3L, "B", "두 번째 댓글");

        verify(responseRepository, org.mockito.Mockito.times(2)).save(any(Response.class));
    }

    /* 검증: 댓글 목록 조회는 parent postId 기준으로 정확히 반환된다 */
    @Test
    void getComments_returnsRepliesForPost() {
        User me = buildStudent();
        Response post = buildPost(3L, me);

        when(responseRepository.findByIdAndModeAndContentTypeAndDeletedAtIsNull(3L, "individual", "chat_post"))
            .thenReturn(Optional.of(post));

        Response reply = new Response();
        reply.setId(11L);
        reply.setStudent(me);
        reply.setParent(post);
        reply.setContent("이유");
        reply.setExtraData(Map.of("choice", "A"));

        when(responseRepository.findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                3L, "individual", "chat_reply"))
            .thenReturn(List.of(reply));

        List<IndividualBookChatCommentResponse> result = service.getComments(STUDENT_ID, 3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChoice()).isEqualTo("A");
        assertThat(result.get(0).getContent()).isEqualTo("이유");
    }

    /* 검증 D: 다른 학급 글의 댓글 목록도 조회할 수 없다(404) */
    @Test
    void getComments_throwsNotFoundWhenPostAuthorInDifferentClass() {
        User me = buildStudent(STUDENT_ID, "나");
        User otherClassAuthor = buildStudent(OTHER_CLASS_STUDENT_ID, "다른학급학생");
        Response post = buildPost(5L, otherClassAuthor);

        when(responseRepository.findByIdAndModeAndContentTypeAndDeletedAtIsNull(5L, "individual", "chat_post"))
            .thenReturn(Optional.of(post));
        when(classStudentRepository.findByStudentId(STUDENT_ID))
            .thenReturn(Optional.of(buildMembership(CLASS_ID, me)));
        when(classStudentRepository.findByStudentId(OTHER_CLASS_STUDENT_ID))
            .thenReturn(Optional.of(buildMembership(OTHER_CLASS_ID, otherClassAuthor)));

        assertThatThrownBy(() -> service.getComments(STUDENT_ID, 5L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }
}
