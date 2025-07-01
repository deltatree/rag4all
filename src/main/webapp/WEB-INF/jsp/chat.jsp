<%--<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>--%>
<%--<%@ page import="org.springframework.security.core.context.SecurityContextHolder,--%>
<%--                  org.springframework.security.core.Authentication" %>--%>
<%--<!DOCTYPE html>--%>
<%--<html>--%>
<%--<head>--%>
<%--    <title>AI Chat Demo</title>--%>
<%--    <%--%>
<%--        // Obtain the authentication object and username from the Spring Security context.--%>
<%--        Authentication auth = SecurityContextHolder.getContext().getAuthentication();--%>
<%--        String username = (auth != null) ? auth.getName() : "";--%>
<%--        String contextPath = request.getContextPath();--%>
<%--    %>--%>
<%--    <link rel="stylesheet" href="<%= contextPath %>/resources/css/style.css"/>--%>
<%--    <script>--%>
<%--        var username = "<%= username %>";--%>
<%--    </script>--%>
<%--    <script src="<%= contextPath %>/resources/js/script.js"></script>--%>
<%--</head>--%>
<%--<body>--%>
<%--<div class="chat-container">--%>
<%--    <!-- Upload Modal -->--%>
<%--    <div id="uploadModal" class="modal">--%>
<%--        <div class="modal-content">--%>
<%--            <span class="closeModalSpan">&times;</span>--%>
<%--            <h2>Upload a file</h2>--%>
<%--            <form id="uploadForm" method="post" action="<%= contextPath %>/upload" enctype="multipart/form-data" target="hiddenUploadFrame">--%>
<%--                <input type="file" name="file" id="file" required/>--%>
<%--                <input type="submit" value="Upload" class="submit-btn"/>--%>
<%--            </form>--%>
<%--            <div class="loader" id="loader">--%>
<%--                <div class="loading-spinner"></div>--%>
<%--            </div>--%>
<%--        </div>--%>
<%--    </div>--%>
<%--    <iframe name="hiddenUploadFrame" id="hiddenUploadFrame" style="display:none;"></iframe>--%>

<%--    <!-- Chat Area -->--%>
<%--    <div id="chatArea">--%>
<%--        <div id="header">--%>
<%--            <h2>AI Chat Demo</h2>--%>
<%--            <div class="user-info">--%>
<%--                Welcome, <%= username %>!--%>
<%--                <form action="<%= contextPath %>/logout" method="post" style="display: inline;">--%>
<%--                    <input type="submit" value="Logout" class="logout-btn"/>--%>
<%--                </form>--%>
<%--            </div>--%>
<%--        </div>--%>

<%--        <div id="transcript"></div>--%>

<%--        <div id="controls">--%>
<%--            <button id="uploadFile" class="upload-btn">Upload File</button>--%>
<%--        </div>--%>

<%--        <div class="input-area">--%>
<%--            <textarea id="userInput" placeholder="Type your question here..." rows="3"></textarea>--%>
<%--            <button id="typedTextSubmit" class="submit-btn">Send</button>--%>
<%--        </div>--%>
<%--    </div>--%>
<%--</div>--%>
<%--</body>--%>
<%--</html>--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.springframework.security.core.context.SecurityContextHolder,
                  org.springframework.security.core.Authentication" %>
<!DOCTYPE html>
<html>
<head>
    <title>AI Chat Demo</title>
    <%
        // Obtain the authentication object and username from the Spring Security context.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        String username = (auth != null) ? auth.getName() : "";
        String username = "User";
        String contextPath = request.getContextPath();
    %>
    <link rel="stylesheet" href="<%= contextPath %>/resources/css/style.css"/>
    <script>
        var username = "<%= username %>";
        var contextPath = "<%= contextPath %>";
    </script>
    <script src="<%= contextPath %>/resources/js/script.js"></script>
</head>
<body>
<div class="chat-container">
    <!-- Upload Modal -->
    <div id="uploadModal" class="modal">
        <div class="modal-content">
            <span class="closeModalSpan">&times;</span>
            <h2>Upload a file</h2>
            <form id="uploadForm" method="post" action="<%= contextPath %>/upload" enctype="multipart/form-data" target="hiddenUploadFrame">
                <input type="file" name="file" id="file" required/>
                <input type="submit" value="Upload" class="submit-btn"/>
            </form>
            <div class="loader" id="loader">
                <div class="loading-spinner"></div>
            </div>
        </div>
    </div>

    <!-- URL Modal -->
    <div id="urlModal" class="modal">
        <div class="modal-content">
            <span class="closeUrlModalSpan">&times;</span>
            <h2>Process Website URL</h2>
            <form id="urlForm">
                <input type="url" name="url" id="urlInput" placeholder="https://example.com" required/>
                <input type="submit" value="Process" class="submit-btn"/>
            </form>
            <div class="loader" id="urlLoader">
                <div class="loading-spinner"></div>
            </div>
        </div>
    </div>

    <iframe name="hiddenUploadFrame" id="hiddenUploadFrame" style="display:none;"></iframe>

    <!-- Chat Area -->
    <div id="chatArea">
        <div id="header">
            <h2>AI Chat Demo</h2>
            <div class="user-info">
                Welcome, <%= username %>!
<%--                <form action="<%= contextPath %>/logout" method="post" style="display: inline;">--%>
<%--                    <input type="submit" value="Logout" class="logout-btn"/>--%>
<%--                </form>--%>
            </div>
        </div>

        <div id="transcript"></div>
        <%if(request.getServerName().equals("localhost") ){%>
        <div id="controls">
            <button id="uploadFile" class="upload-btn">Upload File</button>
            <button id="processUrl" class="upload-btn">Process URL</button>
        </div>
        <%}%>
        <div class="input-area">
            <textarea id="userInput" placeholder="Type your question here..." rows="3"></textarea>
            <button id="typedTextSubmit" class="submit-btn">Send</button>
        </div>
    </div>
</div>
</body>
</html>