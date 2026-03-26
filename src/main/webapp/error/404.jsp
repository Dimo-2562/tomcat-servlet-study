<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html>
<head><title>404 Not Found</title></head>
<body>
<h2>404 - Page Not Found</h2>
<p>Requested URL: <%= request.getAttribute("jakarta.servlet.error.request_uri") %></p>
</body>
</html>