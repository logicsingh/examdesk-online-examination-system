package edu.bsc.exam;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.mindrot.jbcrypt.BCrypt;

@WebServlet(urlPatterns={"/login","/register","/logout","/dashboard","/admin","/admin/users","/admin/subjects","/faculty/exams","/faculty/exam/create","/faculty/question/add","/faculty/exam/publish","/exams/start","/exams/take","/exams/submit","/results"})
public class AppServlet extends HttpServlet {
 private User current(HttpServletRequest req){ HttpSession s=req.getSession(false); return s==null?null:(User)s.getAttribute("user"); }
 private void require(HttpServletRequest req,HttpServletResponse resp,String... roles)throws IOException {
  User u=current(req); if(u==null){resp.sendRedirect(req.getContextPath()+"/login");return;}
  if(roles.length>0 && Arrays.stream(roles).noneMatch(r->r.equals(u.role()))){resp.sendError(403,"You do not have permission to access this page.");}
 }
 private boolean allowed(HttpServletRequest req,HttpServletResponse resp,String... roles)throws IOException {
  User u=current(req); if(u==null){resp.sendRedirect(req.getContextPath()+"/login");return false;}
  if(roles.length>0 && Arrays.stream(roles).noneMatch(r->r.equals(u.role()))){resp.sendError(403,"You do not have permission to access this page.");return false;} return true;
 }
 private void view(HttpServletRequest req,HttpServletResponse resp,String jsp)throws ServletException,IOException {req.getRequestDispatcher("/WEB-INF/views/"+jsp).forward(req,resp);}
 private String val(HttpServletRequest req,String key){String v=req.getParameter(key);return v==null?"":v.trim();}
 private long number(HttpServletRequest req,String key){return Long.parseLong(val(req,key));}
 private String bounded(String s,int max){return s.length()>max?s.substring(0,max):s;}
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
  String p=req.getServletPath();
  if(p.equals("/logout")){HttpSession s=req.getSession(false);if(s!=null)s.invalidate();resp.sendRedirect(req.getContextPath()+"/login?message=Signed+out");return;}
  if(p.equals("/login")||p.equals("/register")){req.setAttribute("mode",p.equals("/register")?"register":"login");view(req,resp,"auth.jsp");return;}
  if(p.equals("/dashboard")){if(!allowed(req,resp))return;dashboard(req,resp);return;}
  if(p.equals("/faculty/exams")){if(!allowed(req,resp,"FACULTY","ADMIN"))return;faculty(req,resp);return;}
  if(p.equals("/admin")){if(!allowed(req,resp,"ADMIN"))return;admin(req,resp);return;}
  if(p.equals("/results")){if(!allowed(req,resp,"STUDENT","FACULTY","ADMIN"))return;results(req,resp);return;}
  if(p.equals("/exams/start")){if(!allowed(req,resp,"STUDENT"))return;start(req,resp);return;}
  if(p.equals("/exams/take")){if(!allowed(req,resp,"STUDENT"))return;take(req,resp);return;}
  resp.sendError(404);
 }
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
  req.setCharacterEncoding("UTF-8"); String p=req.getServletPath();
  try {
   switch(p){
    case "/register" -> register(req,resp);
    case "/login" -> login(req,resp);
    case "/admin/users" -> {if(allowed(req,resp,"ADMIN"))updateRole(req,resp);}
    case "/admin/subjects" -> {if(allowed(req,resp,"ADMIN"))addSubject(req,resp);}
    case "/faculty/exam/create" -> {if(allowed(req,resp,"FACULTY","ADMIN"))createExam(req,resp);}
    case "/faculty/question/add" -> {if(allowed(req,resp,"FACULTY","ADMIN"))addQuestion(req,resp);}
    case "/faculty/exam/publish" -> {if(allowed(req,resp,"FACULTY","ADMIN"))publishExam(req,resp);}
    case "/exams/submit" -> {if(allowed(req,resp,"STUDENT"))submit(req,resp);}
    default -> resp.sendError(404);
   }
  } catch(IllegalArgumentException ex){resp.sendError(400,"Invalid form values. Check the fields and try again.");}
  catch(SQLException ex){throw new ServletException("Database operation failed. Check the database setup and application logs.",ex);}
 }
 private void register(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException,SQLException {
  String name=bounded(val(req,"fullName"),100), email=bounded(val(req,"email").toLowerCase(Locale.ROOT),150), password=req.getParameter("password");
  if(name.length()<2||!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||password==null||password.length()<8||password.length()>72){req.setAttribute("error","Enter a valid name and email. Password must be 8–72 characters.");req.setAttribute("mode","register");view(req,resp,"auth.jsp");return;}
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,'STUDENT')")){
   ps.setString(1,name);ps.setString(2,email);ps.setString(3,BCrypt.hashpw(password,BCrypt.gensalt(12)));ps.executeUpdate();resp.sendRedirect(req.getContextPath()+"/login?message=Account+created.+Sign+in+to+continue");
  }catch(SQLIntegrityConstraintViolationException ex){req.setAttribute("error","An account with this email already exists.");req.setAttribute("mode","register");view(req,resp,"auth.jsp");}
 }
 private void login(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException,SQLException {
  String email=val(req,"email").toLowerCase(Locale.ROOT), password=req.getParameter("password");
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("SELECT id,full_name,email,password_hash,role,active FROM users WHERE email=?")){
   ps.setString(1,email);try(ResultSet rs=ps.executeQuery()){
    if(!rs.next()||!rs.getBoolean("active")||password==null||!BCrypt.checkpw(password,rs.getString("password_hash"))){req.setAttribute("error","Email or password is incorrect.");req.setAttribute("mode","login");view(req,resp,"auth.jsp");return;}
    HttpSession old=req.getSession(false);if(old!=null)old.invalidate();HttpSession s=req.getSession(true);s.setMaxInactiveInterval(1800);s.setAttribute("user",new User(rs.getLong("id"),rs.getString("full_name"),rs.getString("email"),rs.getString("role")));resp.sendRedirect(req.getContextPath()+"/dashboard");
   }
  }
 }
 private void dashboard(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
  User u=current(req);List<Map<String,Object>> exams=new ArrayList<>();int attempts=0;
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("SELECT e.id,e.title,e.duration_minutes,e.total_marks,s.subject_name,(SELECT COUNT(*) FROM attempts a WHERE a.exam_id=e.id AND a.student_id=?) attempted FROM exams e JOIN subjects s ON s.id=e.subject_id WHERE e.status='PUBLISHED' ORDER BY e.created_at DESC")){
   ps.setLong(1,u.id());try(ResultSet rs=ps.executeQuery()){while(rs.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("title",rs.getString("title"));m.put("subject",rs.getString("subject_name"));m.put("duration",rs.getInt("duration_minutes"));m.put("marks",rs.getBigDecimal("total_marks"));m.put("attempted",rs.getInt("attempted")>0);exams.add(m);}}
   try(PreparedStatement a=c.prepareStatement("SELECT COUNT(*) FROM attempts WHERE student_id=? AND status<>'IN_PROGRESS'")){a.setLong(1,u.id());try(ResultSet rs=a.executeQuery()){if(rs.next())attempts=rs.getInt(1);}}
  }catch(SQLException ex){throw new ServletException(ex);}
  req.setAttribute("exams",exams);req.setAttribute("attemptCount",attempts);view(req,resp,"dashboard.jsp");
 }
 private void admin(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
  List<Map<String,Object>> users=new ArrayList<>(),subjects=new ArrayList<>();
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("SELECT id,full_name,email,role,active FROM users ORDER BY created_at DESC");ResultSet rs=ps.executeQuery()){
   while(rs.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("name",rs.getString("full_name"));m.put("email",rs.getString("email"));m.put("role",rs.getString("role"));m.put("active",rs.getBoolean("active"));users.add(m);}
   try(PreparedStatement sp=c.prepareStatement("SELECT id,subject_code,subject_name FROM subjects ORDER BY subject_name");ResultSet sr=sp.executeQuery()){while(sr.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",sr.getLong("id"));m.put("code",sr.getString("subject_code"));m.put("name",sr.getString("subject_name"));subjects.add(m);}}
  }catch(SQLException ex){throw new ServletException(ex);}req.setAttribute("users",users);req.setAttribute("subjects",subjects);view(req,resp,"admin.jsp");
 }
 private void updateRole(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException {
  long id=number(req,"userId");String role=val(req,"role");if(!List.of("ADMIN","FACULTY","STUDENT").contains(role)){resp.sendError(400);return;}
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("UPDATE users SET role=? WHERE id=?")){ps.setString(1,role);ps.setLong(2,id);ps.executeUpdate();}resp.sendRedirect(req.getContextPath()+"/admin?message=Role+updated");
 }
 private void addSubject(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException {
  String code=bounded(val(req,"code").toUpperCase(Locale.ROOT),20),name=bounded(val(req,"name"),120);if(code.isBlank()||name.isBlank()){resp.sendError(400);return;}
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO subjects(subject_code,subject_name) VALUES(?,?)")){ps.setString(1,code);ps.setString(2,name);ps.executeUpdate();}resp.sendRedirect(req.getContextPath()+"/admin?message=Subject+added");
 }
 private void faculty(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
  List<Map<String,Object>> subjects=new ArrayList<>(),exams=new ArrayList<>();User u=current(req);
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("SELECT id,subject_code,subject_name FROM subjects ORDER BY subject_name");ResultSet rs=ps.executeQuery()){
   while(rs.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",rs.getLong("id"));m.put("code",rs.getString("subject_code"));m.put("name",rs.getString("subject_name"));subjects.add(m);}
   String sql="SELECT e.id,e.title,e.status,e.duration_minutes,s.subject_name,(SELECT COUNT(*) FROM questions q WHERE q.exam_id=e.id) question_count FROM exams e JOIN subjects s ON s.id=e.subject_id "+("ADMIN".equals(u.role())?"":"WHERE e.created_by=? ")+"ORDER BY e.created_at DESC";
   try(PreparedStatement ep=c.prepareStatement(sql)){if(!"ADMIN".equals(u.role()))ep.setLong(1,u.id());try(ResultSet er=ep.executeQuery()){while(er.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",er.getLong("id"));m.put("title",er.getString("title"));m.put("status",er.getString("status"));m.put("duration",er.getInt("duration_minutes"));m.put("subject",er.getString("subject_name"));m.put("count",er.getInt("question_count"));exams.add(m);}}}
  }catch(SQLException ex){throw new ServletException(ex);}req.setAttribute("subjects",subjects);req.setAttribute("exams",exams);view(req,resp,"faculty.jsp");
 }
 private void createExam(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException {
  String title=bounded(val(req,"title"),150),instructions=bounded(val(req,"instructions"),2000);long subject=number(req,"subjectId");int duration=Integer.parseInt(val(req,"duration"));User u=current(req);
  if(title.isBlank()||duration<1||duration>300){resp.sendError(400,"Exam title and duration are required.");return;}
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("INSERT INTO exams(subject_id,title,instructions,duration_minutes,status,created_by) VALUES(?,?,?,?,'DRAFT',?)")){ps.setLong(1,subject);ps.setString(2,title);ps.setString(3,instructions);ps.setInt(4,duration);ps.setLong(5,u.id());ps.executeUpdate();}resp.sendRedirect(req.getContextPath()+"/faculty/exams?message=Draft+created");
 }
 private void addQuestion(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException {
  long exam=number(req,"examId");String q=bounded(val(req,"question"),4000),a=bounded(val(req,"optionA"),500),b=bounded(val(req,"optionB"),500),c1=bounded(val(req,"optionC"),500),d=bounded(val(req,"optionD"),500),correct=val(req,"correctOption");BigDecimal marks=new BigDecimal(val(req,"marks"));User u=current(req);
  if(q.isBlank()||a.isBlank()||b.isBlank()||c1.isBlank()||d.isBlank()||!List.of("A","B","C","D").contains(correct)||marks.signum()<=0||marks.compareTo(new BigDecimal("100"))>0){resp.sendError(400,"Complete every question field and enter valid marks.");return;}
  try(Connection con=Database.getConnection();PreparedStatement check=con.prepareStatement("SELECT status FROM exams WHERE id=? AND (created_by=? OR ?='ADMIN')")){
   check.setLong(1,exam);check.setLong(2,u.id());check.setString(3,u.role());try(ResultSet rs=check.executeQuery()){if(!rs.next()||!"DRAFT".equals(rs.getString(1))){resp.sendError(400,"Only your draft exams can be edited.");return;}}
   try(PreparedStatement ps=con.prepareStatement("INSERT INTO questions(exam_id,question_text,option_a,option_b,option_c,option_d,correct_option,marks) VALUES(?,?,?,?,?,?,?,?)")){ps.setLong(1,exam);ps.setString(2,q);ps.setString(3,a);ps.setString(4,b);ps.setString(5,c1);ps.setString(6,d);ps.setString(7,correct);ps.setBigDecimal(8,marks);ps.executeUpdate();}
  }resp.sendRedirect(req.getContextPath()+"/faculty/exams?message=Question+added");
 }
 private void publishExam(HttpServletRequest req,HttpServletResponse resp)throws SQLException,IOException {
  long exam=number(req,"examId");User u=current(req);String sql="UPDATE exams e SET e.total_marks=(SELECT COALESCE(SUM(q.marks),0) FROM questions q WHERE q.exam_id=e.id),e.status='PUBLISHED' WHERE e.id=? AND e.status='DRAFT' AND (e.created_by=? OR ?='ADMIN') AND EXISTS(SELECT 1 FROM questions q WHERE q.exam_id=e.id)";
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,exam);ps.setLong(2,u.id());ps.setString(3,u.role());if(ps.executeUpdate()!=1){resp.sendError(400,"Exam could not be published. Add a question and confirm you own the draft.");return;}}resp.sendRedirect(req.getContextPath()+"/faculty/exams?message=Exam+published");
 }
 private void start(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
  long exam;try{exam=number(req,"id");}catch(Exception ex){resp.sendError(400,"Invalid exam ID");return;}User u=current(req);
  try(Connection c=Database.getConnection()){
   c.setAutoCommit(false);
   try(PreparedStatement ps=c.prepareStatement("SELECT id,status,total_marks FROM exams WHERE id=? FOR UPDATE")){ps.setLong(1,exam);try(ResultSet rs=ps.executeQuery()){
    if(!rs.next()||!"PUBLISHED".equals(rs.getString("status"))){c.rollback();resp.sendError(404,"Exam not available");return;}
    try(PreparedStatement old=c.prepareStatement("SELECT id FROM attempts WHERE exam_id=? AND student_id=?")){old.setLong(1,exam);old.setLong(2,u.id());try(ResultSet ar=old.executeQuery()){if(ar.next()){c.commit();resp.sendRedirect(req.getContextPath()+"/exams/take?id="+exam);return;}}}
    try(PreparedStatement ins=c.prepareStatement("INSERT INTO attempts(exam_id,student_id,max_score) VALUES(?,?,?)")){ins.setLong(1,exam);ins.setLong(2,u.id());ins.setBigDecimal(3,rs.getBigDecimal("total_marks"));ins.executeUpdate();}
   }}c.commit();resp.sendRedirect(req.getContextPath()+"/exams/take?id="+exam);
  }catch(SQLException ex){if("23000".equals(ex.getSQLState())){resp.sendRedirect(req.getContextPath()+"/exams/take?id="+exam);return;}throw new ServletException(ex);}
 }
 private void take(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
  long exam;try{exam=number(req,"id");}catch(Exception ex){resp.sendError(400);return;}User u=current(req);long attempt;Timestamp started;int duration;String title;String instructions;
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("SELECT a.id,a.started_at,a.status,e.duration_minutes,e.title,e.instructions FROM attempts a JOIN exams e ON e.id=a.exam_id WHERE a.exam_id=? AND a.student_id=?")){
   ps.setLong(1,exam);ps.setLong(2,u.id());try(ResultSet rs=ps.executeQuery()){if(!rs.next()){resp.sendRedirect(req.getContextPath()+"/exams/start?id="+exam);return;}attempt=rs.getLong("id");started=rs.getTimestamp("started_at");duration=rs.getInt("duration_minutes");title=rs.getString("title");instructions=rs.getString("instructions");if(!"IN_PROGRESS".equals(rs.getString("status"))){resp.sendRedirect(req.getContextPath()+"/results");return;}}
   long seconds=Math.max(0,duration*60L-Duration.between(started.toLocalDateTime(),LocalDateTime.now()).getSeconds());if(seconds<=0){submitAttempt(c,attempt,exam,u.id(),Map.of(),"AUTO_SUBMITTED");resp.sendRedirect(req.getContextPath()+"/results");return;}
   List<Map<String,Object>> qs=new ArrayList<>();try(PreparedStatement q=c.prepareStatement("SELECT id,question_text,option_a,option_b,option_c,option_d,marks FROM questions WHERE exam_id=? ORDER BY sort_order,id")){q.setLong(1,exam);try(ResultSet qr=q.executeQuery()){while(qr.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("id",qr.getLong("id"));m.put("text",qr.getString("question_text"));m.put("a",qr.getString("option_a"));m.put("b",qr.getString("option_b"));m.put("c",qr.getString("option_c"));m.put("d",qr.getString("option_d"));m.put("marks",qr.getBigDecimal("marks"));qs.add(m);}}}
   req.setAttribute("examId",exam);req.setAttribute("title",title);req.setAttribute("instructions",instructions);req.setAttribute("questions",qs);req.setAttribute("secondsLeft",seconds);view(req,resp,"take.jsp");
  }catch(SQLException ex){throw new ServletException("Could not load the exam.",ex);}
 }
 private void submit(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
  long exam;try{exam=number(req,"examId");}catch(Exception ex){resp.sendError(400);return;}User u=current(req);Map<Long,String> submitted=new HashMap<>();Enumeration<String> names=req.getParameterNames();while(names.hasMoreElements()){String n=names.nextElement();if(n.startsWith("q_")){try{String v=val(req,n);long q=Long.parseLong(n.substring(2));if(List.of("A","B","C","D").contains(v))submitted.put(q,v);}catch(NumberFormatException ignored){}}}
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("SELECT id,started_at,status FROM attempts WHERE exam_id=? AND student_id=?")){ps.setLong(1,exam);ps.setLong(2,u.id());try(ResultSet rs=ps.executeQuery()){if(!rs.next()||!"IN_PROGRESS".equals(rs.getString("status"))){resp.sendRedirect(req.getContextPath()+"/results");return;}long attempt=rs.getLong("id");LocalDateTime started=rs.getTimestamp("started_at").toLocalDateTime();int duration;try(PreparedStatement ep=c.prepareStatement("SELECT duration_minutes FROM exams WHERE id=?")){ep.setLong(1,exam);try(ResultSet er=ep.executeQuery()){if(!er.next()){resp.sendError(404);return;}duration=er.getInt(1);}}
   boolean expired=Duration.between(started,LocalDateTime.now()).getSeconds()>duration*60L;submitAttempt(c,attempt,exam,u.id(),expired?Map.of():submitted,expired?"AUTO_SUBMITTED":"SUBMITTED");
  }}catch(SQLException ex){throw new ServletException("Could not submit answers.",ex);}resp.sendRedirect(req.getContextPath()+"/results");
 }
 private void submitAttempt(Connection c,long attempt,long exam,long student,Map<Long,String> submitted,String status)throws SQLException {
  boolean oldAuto=c.getAutoCommit();c.setAutoCommit(false);
  try{
   try(PreparedStatement lock=c.prepareStatement("SELECT status FROM attempts WHERE id=? AND student_id=? FOR UPDATE")){lock.setLong(1,attempt);lock.setLong(2,student);try(ResultSet rs=lock.executeQuery()){if(!rs.next()||!"IN_PROGRESS".equals(rs.getString(1))){c.rollback();return;}}}
   Map<Long,Scoring.Key> key=new LinkedHashMap<>();try(PreparedStatement ps=c.prepareStatement("SELECT id,correct_option,marks FROM questions WHERE exam_id=?")){ps.setLong(1,exam);try(ResultSet rs=ps.executeQuery()){while(rs.next())key.put(rs.getLong("id"),new Scoring.Key(rs.getString("correct_option"),rs.getBigDecimal("marks")));}}
   BigDecimal score=Scoring.score(key,submitted);
   try(PreparedStatement ps=c.prepareStatement("INSERT INTO answers(attempt_id,question_id,selected_option,is_correct,awarded_marks) VALUES(?,?,?,?,?)")){for(var e:key.entrySet()){String selected=submitted.get(e.getKey());boolean correct=selected!=null&&selected.equalsIgnoreCase(e.getValue().correctOption());ps.setLong(1,attempt);ps.setLong(2,e.getKey());ps.setString(3,selected);ps.setBoolean(4,correct);ps.setBigDecimal(5,correct?e.getValue().marks():BigDecimal.ZERO);ps.addBatch();}ps.executeBatch();}
   try(PreparedStatement ps=c.prepareStatement("UPDATE attempts SET status=?,submitted_at=NOW(),score=? WHERE id=? AND student_id=?")){ps.setString(1,status);ps.setBigDecimal(2,score);ps.setLong(3,attempt);ps.setLong(4,student);ps.executeUpdate();}c.commit();
  }catch(SQLException ex){c.rollback();throw ex;}finally{c.setAutoCommit(oldAuto);}
 }
 private void results(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException {
  User u=current(req);List<Map<String,Object>> rows=new ArrayList<>();try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("SELECT e.title,s.subject_name,a.score,a.max_score,a.submitted_at,a.status FROM attempts a JOIN exams e ON e.id=a.exam_id JOIN subjects s ON s.id=e.subject_id WHERE a.student_id=? AND a.status<>'IN_PROGRESS' ORDER BY a.submitted_at DESC")){ps.setLong(1,u.id());try(ResultSet rs=ps.executeQuery()){while(rs.next()){Map<String,Object>m=new LinkedHashMap<>();m.put("title",rs.getString("title"));m.put("subject",rs.getString("subject_name"));m.put("score",rs.getBigDecimal("score"));m.put("max",rs.getBigDecimal("max_score"));m.put("submitted",rs.getTimestamp("submitted_at"));m.put("status",rs.getString("status"));rows.add(m);}}}catch(SQLException ex){throw new ServletException(ex);}req.setAttribute("results",rows);view(req,resp,"results.jsp");
 }
}
