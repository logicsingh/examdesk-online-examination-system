
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,edu.bsc.exam.Html" %>
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Faculty workspace · ExamDesk</title>
<link rel="stylesheet" href="<%=request.getContextPath()%>/assets/style.css">
</head>
<body>
<%@ include file="nav.jspf" %>

<main class="container">
    <div class="page-heading">
        <div>
            <p class="eyebrow">FACULTY WORKSPACE</p>
            <h1>Assessment management</h1>
            <p class="muted">Create a draft, add questions, then publish it for students.</p>
        </div>
    </div>

    <% if(request.getParameter("message") != null) { %>
        <div class="alert success">
            <%=Html.escape(request.getParameter("message"))%>
        </div>
    <% } %>

    <div class="two-col">
        <section class="panel">
            <h2>Create an exam</h2>

            <form method="post"
                  action="<%=request.getContextPath()%>/faculty/exam/create"
                  class="stack">

                <label>
                    Exam title
                    <input name="title" maxlength="150" required
                           placeholder="e.g. Database Management — Unit Test">
                </label>

                <label>
                    Subject
                    <select name="subjectId" required>
                        <option value="">Choose subject</option>
                        <% for(Map<String,Object> s :
                            (List<Map<String,Object>>)request.getAttribute("subjects")) { %>
                            <option value="<%=s.get("id")%>">
                                <%=Html.escape((String)s.get("code"))%> —
                                <%=Html.escape((String)s.get("name"))%>
                            </option>
                        <% } %>
                    </select>
                </label>

                <label>
                    Duration in minutes
                    <input type="number" name="duration"
                           min="1" max="300" value="30" required>
                </label>

                <label>
                    Instructions
                    <textarea name="instructions" rows="3" maxlength="2000"></textarea>
                </label>

                <button class="button primary" type="submit">Create draft</button>
            </form>
        </section>

        <section class="panel">
            <h2>Add a question</h2>
            <p class="muted">Questions can be added to draft exams only.</p>

            <form method="post"
                  action="<%=request.getContextPath()%>/faculty/question/add"
                  class="stack">

                <label>
                    Draft exam
                    <select name="examId" required>
                        <option value="">Choose a draft</option>
                        <% for(Map<String,Object> e :
                            (List<Map<String,Object>>)request.getAttribute("exams")) {
                            if("DRAFT".equals(e.get("status"))) { %>
                                <option value="<%=e.get("id")%>">
                                    <%=Html.escape((String)e.get("title"))%>
                                </option>
                        <%  }
                        } %>
                    </select>
                </label>

                <label>
                    Question
                    <textarea name="question" rows="2" maxlength="4000" required></textarea>
                </label>

                <div class="form-grid">
                    <label>
                        Option A
                        <input name="optionA" maxlength="500" required>
                    </label>
                    <label>
                        Option B
                        <input name="optionB" maxlength="500" required>
                    </label>
                    <label>
                        Option C
                        <input name="optionC" maxlength="500" required>
                    </label>
                    <label>
                        Option D
                        <input name="optionD" maxlength="500" required>
                    </label>
                </div>

                <div class="form-grid">
                    <label>
                        Correct option
                        <select name="correctOption" required>
                            <option>A</option>
                            <option>B</option>
                            <option>C</option>
                            <option>D</option>
                        </select>
                    </label>

                    <label>
                        Marks
                        <input type="number" name="marks"
                               min="0.01" max="100" step="0.01"
                               value="1" required>
                    </label>
                </div>

                <button class="button primary" type="submit">Add question</button>
            </form>
        </section>
    </div>

    <section class="panel table-panel">
        <div class="section-heading">
            <div>
                <h2>Assessments</h2>
                <p class="muted">Publish an exam only after adding at least one question.</p>
            </div>
        </div>

        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Exam</th>
                        <th>Subject</th>
                        <th>Questions</th>
                        <th>Duration</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    <% for(Map<String,Object> e :
                        (List<Map<String,Object>>)request.getAttribute("exams")) { %>
                        <tr>
                            <td>
                                <strong><%=Html.escape((String)e.get("title"))%></strong>
                            </td>
                            <td><%=Html.escape((String)e.get("subject"))%></td>
                            <td><%=e.get("count")%></td>
                            <td><%=e.get("duration")%> min</td>
                            <td>
                                <span class="status <%=((String)e.get("status")).toLowerCase()%>">
                                    <%=e.get("status")%>
                                </span>
                            </td>
                            <td>
                                <% if("DRAFT".equals(e.get("status"))
                                      && ((Integer)e.get("count")) > 0) { %>
                                    <form method="post"
                                          action="<%=request.getContextPath()%>/faculty/exam/publish">
                                        <input type="hidden" name="examId"
                                               value="<%=e.get("id")%>">
                                        <button class="button small primary" type="submit">
                                            Publish
                                        </button>
                                    </form>
                                <% } else { %>
                                    —
                                <% } %>
                            </td>
                        </tr>
                    <% } %>
                </tbody>
            </table>
        </div>
    </section>
</main>

<%@ include file="footer.jspf" %>
</body>
</html>
