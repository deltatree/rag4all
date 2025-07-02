<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="https://jakarta.ee/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Embedded Documents</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/resources/css/style.css" />
</head>
<body>
<h1>Embedded Documents</h1>
<table border="1" cellpadding="5" cellspacing="0">
    <tr>
        <th>ID</th>
        <th>File Name</th>
        <th>Created At</th>
        <th>Actions</th>
    </tr>
    <c:forEach var="doc" items="${documents}">
        <tr>
            <td>${doc.id}</td>
            <td>${doc.fileName}</td>
            <td>${doc.createdAt}</td>
            <td>
                <form method="post" action="<%= request.getContextPath() %>/documents/delete/${doc.id}" style="display:inline;">
                    <input type="submit" value="Delete" class="submit-btn" />
                </form>
            </td>
        </tr>
    </c:forEach>
</table>
</body>
</html>
