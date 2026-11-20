import com.samskivert.mustache.Mustache;
import java.nio.file.*;
import java.util.*;

// Run: java -cp <jmustache.jar> scripts/FinalPolishRender.java
// Fixtures exercise the real partials without touching a database or payment service.
class FinalPolishRender {
    static final Path ROOT = Path.of("src/main/resources/templates");
    static final Path OUT = Path.of("build/final-polish");
    static Map<String, Object> map(Object... pairs) {
        var result = new HashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
    static Map<String, Object> base(boolean host) {
        return map("isPersonal", !host, "isCorporate", host, "isAdmin", false,
                "loginUser", map("displayName", "부산테스터"));
    }
    static void render(String name, String template, Map<String, Object> model) throws Exception {
        var compiler = Mustache.compiler().defaultValue("")
                .withLoader(n -> Files.newBufferedReader(ROOT.resolve(n + ".mustache")));
        Files.writeString(OUT.resolve(name + ".html"), compiler.compile(Files.readString(
                ROOT.resolve(template + ".mustache"))).execute(model));
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        for (boolean host : List.of(false, true)) {
            var model = base(host);
            model.put("myPoint", 28500);
            render(host ? "charge-company" : "charge-personal", "point/charge-point", model);
            model.put("room", map("sessionId", 12, "eventTitle", "부산 문화 체험 행사", "sessionName", "오전 회차",
                    "hostName", "부산문화", "myKey", host ? "company:5" : "user:3", "host", host,
                    "participantCount", 2, "participants", List.of(map("userId", 3, "nickname", "참가자")),
                    "notice", "행사 시작 10분 전까지 입장해주세요.", "empty", true, "messages", List.of()));
            render(host ? "community-host" : "community-member", "community/room", model);
        }
        var qr = base(true);
        qr.put("qr", map("eventTitle", "부산 문화 체험 행사", "sessionName", "오전 회차", "scheduleText", "2030.12.01",
                "checkPath", "/attendance/check/12", "attendedCount", 1, "confirmedCount", 2));
        render("qr", "attendance/qr", qr);
        for (String category : List.of("", "1", "2", "empty")) {
            for (int page : List.of(1, 2)) {
                var model = base(false);
                model.put("categoryId", category);
                model.put("allCategories", category.isEmpty());
                model.put("keyword", "");
                model.put("isSearch", false);
                model.put("categories", List.of(map("id", 1, "name", "전시·체험", "selected", category.equals("1")),
                        map("id", 2, "name", "공연·축제", "selected", category.equals("2")),
                        map("id", "empty", "name", "빈 카테고리", "selected", category.equals("empty"))));
                var event = map("id", 101, "title", "행사 " + category + " 페이지 " + page,
                        "imageUrl", "/event-files/101", "summary", "부산에서 만나는 문화 행사", "categoryName", "전시·체험",
                        "sessionCount", 1, "sessionPeriods", List.of(map("name", "오전", "applyStartAt", "2030.11.01", "applyEndAt", "2030.11.30")));
                model.put("eventPage", map("eventList", category.equals("empty") ? List.of() : List.of(event),
                        "currentPage", page, "totalPages", category.equals("empty") ? 0 : 2,
                        "totalElements", category.equals("empty") ? 0 : 2, "size", 10));
                render("list-" + (category.isEmpty() ? "all" : category) + "-" + page, "event/list", model);
                if (category.isEmpty() && page == 1) render("main", "main", model);
            }
        }
        System.out.println("Rendered final-polish fixtures from current Mustache templates");
    }
}
