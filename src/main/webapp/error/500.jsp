<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html>
<head><title>500 Internal Server Error</title></head>
<body>
<h2>500 - Internal Server Error</h2>
<p>Exception: <%= request.getAttribute("jakarta.servlet.error.exception") %></p>
<p>Requested URL: <%= request.getAttribute("jakarta.servlet.error.request_uri") %></p>
</body>
</html>