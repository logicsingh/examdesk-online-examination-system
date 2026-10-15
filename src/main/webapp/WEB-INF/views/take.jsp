
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,edu.bsc.exam.Html" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Take exam Â· ExamDesk</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>

<header class="exam-bar">
    <a class="brand" href="<%= request.getContextPath() %>/dashboard">
        <span class="brand-mark">E</span> ExamDesk
    </a>
    <div>
        <span class="muted">Time remaining</span>
        <strong id="timer" class="timer">--:--</strong>
    </div>
</header>

<main class="container exam-container">
    <div class="page-heading">
        <div>
            <p class="eyebrow">ASSESSMENT IN PROGRESS</p>
            <h1><%= Html.escape((String) request.getAttribute("title")) %></h1>
            <p class="muted">
                <%= Html.escape((String) request.getAttribute("instructions")) %>
            </p>
        </div>
    </div>

    <form id="examForm" method="post"
          action="<%= request.getContextPath() %>/exams/submit">

        <input type="hidden" name="_csrf"
               value="<%= Html.escape((String) session.getAttribute("csrfToken")) %>">

        <input type="hidden" name="examId"
               value="<%= request.getAttribute("examId") %>">

        <%
        int n = 0;
        List<Map<String, Object>> questions =
            (List<Map<String, Object>>) request.getAttribute("questions");

        if (questions != null) {
            for (Map<String, Object> q : questions) {
                n++;
        %>
            <section class="question-card">
                <div class="question-number">
                    QUESTION <%= n %>
                    <span><%= q.get("marks") %> marks</span>
                </div>

                <h3><%= Html.escape((String) q.get("text")) %></h3>

                <div class="options">
                    <%
                    String[] labs = {"A", "B", "C", "D"};
                    String[] ks = {"a", "b", "c", "d"};

                    for (int i = 0; i < 4; i++) {
                    %>
                        <label class="option">
                            <input type="radio"
                                   name="q_<%= q.get("id") %>"
                                   value="<%= labs[i] %>">

                            <span class="option-letter"><%= labs[i] %></span>
                            <span><%= Html.escape((String) q.get(ks[i])) %></span>
                        </label>
                    <% } %>
                </div>
            </section>
        <%
            }
        }
        %>

        <div class="submit-bar">
            <p class="muted">Unanswered questions receive zero marks.</p>
            <button class="button primary" type="submit"
                    onclick="return confirm('Submit your exam now? You cannot change answers afterwards.');">
                Submit exam
            </button>
        </div>
    </form>
</main>

<script>
(function () {
    let remaining = Number('<%= request.getAttribute("secondsLeft") %>');
    const timer = document.getElementById('timer');
    const form = document.getElementById('examForm');
    let sent = false;

    function tick() {
        const m = Math.floor(remaining / 60);
        const s = remaining % 60;

        timer.textContent =
            String(m).padStart(2, '0') + ':' +
            String(s).padStart(2, '0');

        if (remaining <= 0 && !sent) {
            sent = true;
            form.submit();
            return;
        }

        remaining--;
    }

    tick();
    setInterval(tick, 1000);

    form.addEventListener('submit', function () {
        sent = true;
    });
})();
</script>

</body>
</html>
