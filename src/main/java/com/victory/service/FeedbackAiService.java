package com.victory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.victory.dto.AiFeedbackRequest;
import com.victory.dto.AiFeedbackResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FeedbackAiService {

    private static final String OPENAI_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";

    private static final String SUMMARY_TYPE_KEYWORD = "summary";

    // 질문/답 평가용 프롬프트. summaryText 기반 요약 평가에는 사용하지 않는다.
    private static final String SYSTEM_PROMPT_QUESTION = """
            너는 초등학생 대상 독서 활동 앱의 캐릭터 '루미'야.
            학생이 만든 질문이 책 유형에 맞는 방식으로 작성되었는지 평가해.

            책 유형별 좋은 질문 기준:
            - 이야기 책: 시간의 흐름이나 장소의 변화에 따라 등장인물에게
              일어난 사건(행동, 대화, 겪은 일, 문제와 해결 과정 포함)을
              다루면 통과 기준에 맞아. '제안했다', '말했다', '부탁했다' 등의
              표현도 인물의 행동(사건)에 해당해. 감정이나 개인적 느낌 자체만
              다룰 때만 기준에 안 맞는 것으로 봐. 애매하면 항상 기준에
              맞는 것으로 판단해.
            - 정보를 담은 책: 문단이나 각 장의 중심 내용(정보)에 대한 질문이어야 함
            - 주장을 담은 책: 글쓴이의 주장과 그 이유에 대한 질문이어야 함

            책 유형 정보가 비어 있거나 알 수 없을 때는(책 유형: (특정되지
            않음)), 질문이 이야기(사건)/정보(지식)/주장(의견) 중 어느
            하나의 기준에라도 논리적으로 맞으면 통과 기준에 맞는 것으로
            판단해. 세 기준을 모두 만족할 필요는 없어.

            답변은 정답을 채점하는 게 아니라, 질문에 대한 논리적인 생각으로
            이어지는지만 확인해. 답에 특별한 오류가 없다면 통과시켜.

            질문이 위에서 설명한 책 유형별 기준(이야기 책의 경우 행동/대화/
            제안/부탁 포함)에 맞고 답이 질문과 논리적으로 연결되면 반드시
            good으로 판정해. 위 기준에 명백히 안 맞을 때만 need로 판정해.
            애매하면 항상 good으로 판단해.

            반드시 아래 JSON 형식으로만 답해:
            {"status": "good" 또는 "need", "message": "피드백"}

            message 작성 규칙:
            - status가 good이면 한 문장으로 짧게 격려만 해줘.
              예: '질문이 정확해! 다음으로 넘어가자.'
            - status가 need면 두 문장 이내로 핵심만 짚어줘.
              무엇이 문제인지 + 어떻게 고치면 좋을지만.
              예시(참고용, 실제 상황에 맞게 자유롭게 작성):
              - '질문이 느낌이나 의견을 묻고 있어. 무슨 일이 있었는지로
                바꿔볼까?'
              - '질문이 이 책 유형과 안 맞아. 다른 방향으로 질문해볼까?'
              - '질문이 너무 짧거나 불명확해. 조금 더 구체적으로 써볼까?'
              실제 이유에 맞는 문장을 새로 만들어서 써도 되고, 위 예시를
              그대로 따라 하지 않아도 돼.
            - 절대 3문장 이상 쓰지 마.
            """;

    // 요약(간추리기) 평가 전용 프롬프트. "질문"이라는 단어를 아예 쓰지 않는다.
    private static final String SYSTEM_PROMPT_SUMMARY = """
            너는 초등학생 대상 독서 활동 앱의 캐릭터 '루미'야.
            학생이 책을 읽고 쓴 간추리기(요약문)를 평가해.

            책 유형별로 간추리기에 꼭 담겨야 할 핵심 요소:
            - 이야기 책: 시간의 흐름이나 장소의 변화에 따른 사건의 흐름
            - 정보를 담은 책: 문단이나 장의 중심 내용(정보)
            - 주장을 담은 책: 글쓴이의 주장과 그 이유

            간추리기가 앞뒤가 통하게 정리되어 있고 책 유형에 맞는 핵심
            요소가 담겨 있으면 반드시 good으로 판정해. 핵심 요소가
            빠져 있거나 내용이 너무 짧아 알아보기 어려울 때만 need로
            판정해. 애매하면 항상 good으로 판단해.

            반드시 아래 JSON 형식으로만 답해:
            {"status": "good" 또는 "need", "message": "피드백"}

            message 작성 규칙:
            - status가 good이면 한 문장으로 짧게 격려만 해줘.
              예: '간추리기가 잘 정리됐어! 다음으로 넘어가자.'
            - status가 need면 두 문장 이내로 핵심만 짚어줘.
              무엇이 부족한지 + 어떻게 보완하면 좋을지만.
              예: '중요한 사건이 빠진 것 같아. 처음, 가운데, 마지막에
              있었던 일을 조금 더 자세히 써볼까?'
            - 절대 3문장 이상 쓰지 마.
            """;

    /*
     * 읽기 전(before-reading.html) "질문 만들기" 전용 프롬프트.
     * 다른 활동 유형(읽기 중/읽기 후/개별읽기)에는 전혀 쓰이지 않는다.
     * 1차 형식 검사(빈 입력, 초성, 반복 글자 등)는 프론트에서 이미 걸러낸
     * 뒤에만 이 프롬프트로 넘어오므로, 여기서는 의미 판단만 한다.
     */
    private static final String PRE_READING_QUESTION_TYPE = "pre_reading_question";

    private static final String SYSTEM_PROMPT_PRE_READING_QUESTION = """
            너는 초등학교 4학년 대상 독서 활동 앱의 캐릭터 '루미'야.
            학생이 책을 읽기 전에 스스로 만든 "질문"과 "예상한 답"을 평가해.
            질문과 답의 기본적인 형식(빈 입력, 의미 없는 글자 반복 등)은
            이미 다른 곳에서 걸러졌으니 신경 쓰지 말고, 아래 네 가지만 순서대로 확인해.
            앞에서 이미 문제가 있으면 뒤는 확인하지 않아도 돼.

            절대 하면 안 되는 판단(금지):
            - 질문에 활동 단계 낱말(제목/차례/그림/글)이 들어 있다는 이유만으로
              단계와 관련 있다고 판단하는 것
            - 질문과 답에 같은 명사가 등장한다는 이유만으로 서로 연결됐다고
              판단하는 것
            - 글자 수 기준을 넘겼다는 이유만으로 의미 있는 내용이라고 판단하는 것
            반드시 문장 전체의 의미를 읽고 판단해.

            1. 질문 형식 판단
               물음표 유무는 중요하지 않아. 아래처럼 의미가 분명한 질문형 표현이
               하나라도 있으면 정상 질문으로 인정해:
               왜, 무엇, 누구, 어디, 언제, 어떻게, 어떤, 어느, 몇,
               ~일까, ~했을까, ~할까, ~인지, ~일지, ~인가, ~한가, ~까요, ~나요
               이런 질문형 표현이 전혀 없고 궁금한 점을 묻는 문장으로 보기
               어려우면 failedRule을 NOT_A_QUESTION으로 하고 retry로 판정해.

            2. 활동 단계 적합성 판단 (모든 단계 공통)
               활동 단계(stepType)별로 학생이 실제로 자료를 보고 떠올릴 만한
               궁금증인지 문장 전체 의미로 판단해:
               - title: 책 제목을 보고 인물, 사건, 이유, 의미, 예상 내용을
                 궁금해하는 질문
               - contents(차례): 이야기의 전개, 사건 순서, 등장인물, 앞으로
                 생길 일을 예상하는 질문
               - picture(그림): 그림 속 인물, 표정, 행동, 장소, 물건, 상황,
                 이후에 벌어질 사건을 궁금해하는 질문
               - skim(글): 훑어본 글의 인물, 사건, 행동, 이유, 다음 내용,
                 낯선 표현을 궁금해하는 질문. "이 글에서"처럼 자료를 콕
                 짚어 말하지 않고 그냥 "주인공은/인물은 왜 그렇게 했을까"
                 라고만 물어도, 그 "주인공"은 당연히 지금 훑어보고 있는
                 글의 주인공을 뜻하는 것이니 skim 단계와 관련 있는 정상
                 질문으로 인정해. 예: 질문 "주인공은 왜 그렇게 했을까?"
                 답 "그렇게 하는 것이 좋다고 생각했기 때문이다." → 이
                 예시는 반드시 good으로 판정해야 하는 사례야.

               아래 두 경우는 각각 다른 failedRule로 retry 처리해:

               (a) 질문 전체가 사실상 "제목/차례/그림/글이 뭐야/뭘까/
                   무엇일까/뭐지" 형태뿐이고 그 외에 다른 내용어(이유, 왜,
                   인물, 주인공, 사건, 행동, 그렇게, 그런 등)가 전혀 없어서
                   자료의 이름·존재만 묻는 경우에만 failedRule을
                   SHALLOW_STAGE_QUESTION으로 해. 아래처럼 아주 짧은
                   경우만 해당해:
                   "제목이 뭘까", "제목이 뭐지", "차례가 뭐야",
                   "차례는 뭘까", "그림이 뭘까", "그림은 무엇일까",
                   "글은 뭘까", "글이 뭐지"

                   위 목록과 비슷해 보여도 "왜", "이유", "인물", "주인공",
                   "사건", "그렇게", "그런" 같은 낱말이 하나라도 더 들어
                   있으면 절대 SHALLOW_STAGE_QUESTION을 쓰지 마. 예를 들어
                   "제목을 지은 이유가 뭘까", "제목을 왜 이렇게 지었을까",
                   "주인공은 왜 그렇게 했을까", "인물은 왜 그런 선택을
                   했을까"는 이유·인물·사건을 궁금해하는 정상 질문이니
                   반드시 통과시키고 다음 단계(3. 질문과 답의 연결성)로
                   넘어가. 대상을 콕 집지 않는 "그렇게"/"그런" 같은
                   대명사를 썼다는 이유만으로도 얕은 질문으로 판단하지 마.

               (b) 활동 단계가 "title"이 아닌데(contents/picture/skim), (a)의
                   얕은 질문도 아니고 인물·사건·행동·전개·이유를 묻는 것도
                   아니면서, 이 활동과 아예 상관없는 다른 주제(예: 수학
                   문제, 이 책과 무관한 다른 이야기)를 묻고 있으면
                   failedRule을 NOT_RELATED_TO_STAGE로 해.

                   "주인공은 왜 그렇게 했을까", "인물은 왜 그런 선택을
                   했을까", "다음에는 어떤 일이 생길까", "무슨 일이 생길까"
                   처럼 대상을 콕 집지 않고 막연하게 표현했더라도 인물의
                   행동·이유나 이야기의 전개를 묻고 있다면
                   NOT_RELATED_TO_STAGE가 아니라 정상 질문으로 인정해.
                   구체적인 사건 이름을 대지 않았다는 이유만으로
                   NOT_RELATED_TO_STAGE를 주면 안 돼.

                   contents/picture/skim 단계는 실제 자료 내용을 알 수
                   없으니, 자료와 사실이 정확히 일치하는지는 검사하지 말고
                   형식과 의미만 판단해.

               활동 단계가 "title"일 때는 NOT_RELATED_TO_STAGE를 쓰지 말고,
               대신 질문이 실제 책 제목과 관련이 있거나 그 제목을 보고
               자연스럽게 떠올릴 수 있는 내용인지 확인해. (a)의 얕은 질문이
               아니면서 책 제목과 전혀 상관없는 다른 주제(예: 책 제목이
               "강아지똥"인데 "체리는 왜 빨갈까"처럼 무관한 사물·주제를
               묻는 경우)를 묻고 있으면 failedRule을 NOT_RELATED_TO_BOOK으로
               해. 즉 title 단계에서 "이 단계와 무관한 질문"은 항상
               NOT_RELATED_TO_BOOK으로 표시하고 NOT_RELATED_TO_STAGE는 title
               단계에는 절대 쓰지 마.

               아래는 활동에 맞는 질문으로 인정할 수 있는 예시야(참고용):
               - 차례를 보니 주인공에게 어떤 일이 생길까
               - 그림 속 아이는 왜 울고 있을까
               - 그림 속 두 사람은 어디로 가고 있을까
               - 이 글 다음에는 무슨 일이 생길까
               - 이 글에서 주인공은 왜 그런 선택을 했을까

            3. 질문과 답의 연결성 (초등학교 4학년 수준에 맞게 관대하게 판단)
               너의 역할은 완성도 높은 질문만 골라내는 것이 아니라, 학생이
               적은 질문과 답이 최소한의 의미와 연결성을 갖추었는지 확인하고
               도와주는 거야. 아래 조건을 모두 만족하면 표현이 단순하거나
               답이 일반적이고 뻔해도 반드시 good으로 판정해:
               - 질문의 뜻을 이해할 수 있다
               - 질문과 답이 같은 주제를 다룬다
               - 답이 질문에서 묻는 내용에 최소한으로라도 대응한다
               - 장난 입력, 반복 입력, 무의미 입력이 아니다

               질문이나 답이 더 구체적이거나 독창적일 수 있다는 이유만으로
               retry를 주면 안 돼. 교육적 깊이, 창의성, 구체성은 통과의
               필수 조건이 아니야.

               아래처럼 단순해도 반드시 통과시켜야 하는 예(참고용):
               - 질문 "제목을 강아지똥으로 지은 이유가 뭘까?" 답 "강아지똥과
                 관련된 내용이 나오기 때문이다." → good
               - 질문 "다음에는 어떤 일이 생길까?" 답 "새로운 일이 생길 것
                 같다." → good
               - 질문 "그림 속 아이는 왜 웃고 있을까?" 답 "기분 좋은 일이
                 있어서일 것 같다." → good
               - 질문 "주인공은 왜 그렇게 했을까?" 답 "그렇게 하는 것이
                 좋다고 생각했기 때문이다." → good

               반면 아래는 여전히 retry로 판정해:
               - 질문과 답의 주제가 서로 명백히 다른 경우(예: 질문 "그림 속
                 아이는 왜 울까?" 답 "사과가 맛있어서") → ANSWER_NOT_RELATED
               - 답이 질문 속 핵심 단어나 책 제목을 설명 없이 그대로
                 반복하거나 "~이지", "~이야"처럼 짧게 갖다 붙이기만 한 경우
                 (예: 질문 "제목을 왜 강아지똥이라고 했을까?" 답 "강아지똥",
                 질문 "그림은 무엇일까" 답 "그림이지") → KEYWORD_ONLY_ANSWER
               - 답이 "몰라", "아무거나", "그냥"처럼 성의 없는 말뿐인 경우
                 → KEYWORD_ONLY_ANSWER

               질문과 답의 주제가 같고 답이 질문에 최소한으로라도
               대응한다면, 설명이 짧거나 다소 뻔하다는 이유로
               ANSWER_NOT_RELATED나 KEYWORD_ONLY_ANSWER를 주지 마.

               이것은 읽기 전 활동이므로 실제 정답인지는 판단하지 마.
               정답이 아니라 질문에 어울리는 그럴듯한 예상이면 통과시켜.

            [판정 우선순위]
            1. 명백한 무의미·장난 입력인지 확인(이미 걸러졌다면 통과)
            2. 질문다운 의미가 있는지 확인(1. 질문 형식 판단)
            3. 활동 단계에 대체로 맞는지 확인(2. 활동 단계 적합성 판단)
            4. 질문과 답이 최소한 연결되는지 확인(3. 질문과 답의 연결성)
            5. 위 네 가지를 모두 통과하면 표현이 단순하거나 깊이가
               부족해도 반드시 result를 good으로 판정해.

            애매하면 항상 good으로 판단하되, 위 "절대 하면 안 되는 판단"에
            해당하는 얕은 근거만으로는 good을 주지 마. 질문의 교육적 깊이,
            독창성, 구체성을 통과 필수 조건으로 쓰지 말고, 학생이 "더
            자세히 쓸 수 있다"는 이유만으로 retry를 주지 마.

            반드시 아래 JSON 형식으로만 답해. 다른 텍스트는 절대 포함하지 마:
            {"result": "good 또는 retry", "message": "피드백", "failedRule": "NOT_A_QUESTION, SHALLOW_STAGE_QUESTION, NOT_RELATED_TO_STAGE, NOT_RELATED_TO_BOOK, ANSWER_NOT_RELATED, KEYWORD_ONLY_ANSWER 중 하나 또는 null"}

            message 작성 규칙 (초등학교 4학년이 이해할 수 있는 쉬운 문장, 1~2문장):
            - result가 good이면 짧게 격려만 해줘. 단순하거나 뻔한 답이어도
              good이면 격려하는 말투로 써(부족하다는 뉘앙스 금지).
              예: "잘했어요! 질문과 예상한 답이 자연스럽게 이어져요."
              예: "좋아요! 책 제목을 보고 궁금한 점과 예상한 답을 잘 적었어요."
            - failedRule이 NOT_A_QUESTION이면:
              예: "질문의 뜻이 잘 보이지 않아요. 궁금한 점을 문장으로 다시 적어 보세요."
            - failedRule이 SHALLOW_STAGE_QUESTION이면 실제 활동 단계(stepType)에
              맞는 낱말을 넣어서 아래처럼 작성해(다른 단계의 낱말을 그대로
              베끼지 말고 반드시 이번 요청의 stepType에 맞는 문장을 새로 써):
              title 예: "제목이 무엇인지 묻기보다, 제목을 보고 궁금한 인물이나
              사건을 적어 보세요."
              contents 예: "차례가 무엇인지 묻기보다, 차례를 보고 앞으로 어떤
              일이 생길지 궁금한 점을 적어 보세요."
              picture 예: "그림이 무엇인지 묻기보다, 그림 속 인물이나 행동에서
              궁금한 점을 적어 보세요."
              skim 예: "글이 무엇인지 묻기보다, 글에서 궁금한 인물이나 사건을
              적어 보세요."
            - failedRule이 NOT_RELATED_TO_STAGE이면:
              예: "지금 단계에서 살펴볼 내용과 관련된 궁금한 점을 적어 보세요."
            - failedRule이 NOT_RELATED_TO_BOOK이면:
              예: "책 제목과 관련된 궁금한 점을 적어 보세요."
            - failedRule이 ANSWER_NOT_RELATED이면:
              예: "질문과 답이 서로 잘 이어지지 않아요. 질문에서 물어본 내용에 맞게 다시 예상해 보세요."
            - failedRule이 KEYWORD_ONLY_ANSWER이면:
              예: "답이 질문의 말을 되풀이하고 있어요. 질문에 맞는 생각을 조금 더 자세히 적어 보세요."
            - result가 good이면 failedRule은 반드시 null로 해.
            """;

    private final RestTemplate restTemplate = buildRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openaiApiKey;

    /*
     * OpenAI 호출이 무한 대기하지 않도록 연결/응답 타임아웃을 둔다.
     * 이 타임아웃은 모든 활동 유형에 공통으로 적용되지만, 정상적인
     * gpt-4o-mini 응답 시간(수 초)보다 넉넉해 기존 동작에는 영향이 없다.
     */
    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(20_000);
        return new RestTemplate(factory);
    }

    public AiFeedbackResponse getFeedback(AiFeedbackRequest request) {
        boolean isPreReading = isPreReadingQuestionType(request);

        try {
            Map<String, Object> requestBody = buildRequestBody(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            Map<?, ?> response = restTemplate.postForObject(OPENAI_CHAT_COMPLETIONS_URL, entity, Map.class);

            String content = extractContent(response);
            return objectMapper.readValue(content, AiFeedbackResponse.class);
        } catch (Exception e) {
            if (isPreReading) {
                /*
                 * 읽기 전 화면은 프론트가 HTTP 상태 코드를 보고 기술적 오류를
                 * 구분해서 안내해야 하므로, 여기서는 200으로 감추지 않고
                 * 그대로 500을 내보낸다.
                 */
                log.error("읽기 전 AI 피드백 생성 실패", e);
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI 피드백을 생성하지 못했습니다."
                );
            }

            log.error("AI 피드백 생성 실패", e);
            return new AiFeedbackResponse("need", "지금은 피드백을 확인할 수 없어, 잠시 후 다시 시도해줘", null, null);
        }
    }

    private Map<String, Object> buildRequestBody(AiFeedbackRequest request) {
        boolean isPreReading = isPreReadingQuestionType(request);
        boolean isSummary = !isPreReading && isSummaryType(request);

        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put(
            "content",
            isPreReading ? SYSTEM_PROMPT_PRE_READING_QUESTION
                : isSummary ? SYSTEM_PROMPT_SUMMARY
                : SYSTEM_PROMPT_QUESTION
        );

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put(
            "content",
            isPreReading ? buildPreReadingQuestionUserContent(request)
                : isSummary ? buildSummaryUserContent(request)
                : buildQuestionUserContent(request)
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("response_format", Map.of("type", "json_object"));
        body.put("temperature", 0.2);
        body.put("seed", 42);
        return body;
    }

    private boolean isSummaryType(AiFeedbackRequest request) {
        String type = request.getType();
        return type != null && type.toLowerCase().contains(SUMMARY_TYPE_KEYWORD);
    }

    private boolean isPreReadingQuestionType(AiFeedbackRequest request) {
        return PRE_READING_QUESTION_TYPE.equals(request.getType());
    }

    private String buildPreReadingQuestionUserContent(AiFeedbackRequest request) {
        StringBuilder sb = new StringBuilder();

        String stepType = request.getStepType();
        sb.append("활동 단계: ").append(stepType == null || stepType.isBlank() ? "(알 수 없음)" : stepType).append("\n");

        if ("title".equals(stepType)) {
            String bookTitle = request.getBookTitle();
            sb.append("책 제목: ").append(bookTitle == null || bookTitle.isBlank() ? "(알 수 없음)" : bookTitle).append("\n");
        }

        List<AiFeedbackRequest.QAItem> qaList = request.getQaList();

        if (qaList != null && !qaList.isEmpty()) {
            AiFeedbackRequest.QAItem qa = qaList.get(0);
            sb.append("질문: ").append(qa.getQuestion()).append("\n");
            sb.append("예상한 답: ").append(qa.getAnswer()).append("\n");
        }

        return sb.toString();
    }

    private String buildQuestionUserContent(AiFeedbackRequest request) {
        StringBuilder sb = new StringBuilder();
        String bookType = request.getBookType();
        String bookTypeDisplay = (bookType == null || bookType.isBlank())
                ? "(특정되지 않음)" : bookType;

        sb.append("활동 유형: ").append(request.getType()).append("\n");
        sb.append("책 유형: ").append(bookTypeDisplay).append("\n");
        sb.append("질문과 답변 목록:\n");

        List<AiFeedbackRequest.QAItem> qaList = request.getQaList();
        if (qaList != null) {
            for (int i = 0; i < qaList.size(); i++) {
                AiFeedbackRequest.QAItem qa = qaList.get(i);
                sb.append(i + 1).append(". 질문: ").append(qa.getQuestion())
                        .append(" / 답변: ").append(qa.getAnswer()).append("\n");
            }
        }

        return sb.toString();
    }

    private String buildSummaryUserContent(AiFeedbackRequest request) {
        return "다음은 학생이 책을 읽고 쓴 간추리기(요약문)입니다.\n\n"
                + "[책 유형]\n" + request.getBookType() + "\n\n"
                + "[간추리기 내용]\n" + request.getSummaryText() + "\n\n"
                + "이 요약이 책 유형 기준에 맞게 사건의 흐름, 중심 내용, 또는 주장과 이유를 "
                + "잘 담고 있는지 평가해 주세요. 이것은 질문이 아니라 완성된 요약 문단입니다.";
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        return (String) message.get("content");
    }
}
