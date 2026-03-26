<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.Arrays" %>
<!DOCTYPE html>
<html>
<head>
    <title>View</title>
</head>
<body>
<h2>메시지: ${message}</h2>
<ul>
    <% for (String item : (String[]) request.getAttribute("items")) { %>
        <li><%= item %></li>
    <% } %>
</ul>
<p>현재 URL: <%= request.getRequestURI() %></p>
</body>
</html>