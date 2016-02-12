<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html>
<spring:url value="/resources" var="resources"/>
<head>
  	<link rel="stylesheet" media="screen" href="${resources}/css/qunit.css" />
	<jsp:include page="head.jsp"/>
	<script src="${resources}/scripts/qunit.js"></script>
	<script src="${resources}/scripts/tests.js"></script>
	<title>cPath2::QUnit Tests</title>
	<meta name="robots" content="noindex,nofollow" />
</head>
<body>

<jsp:include page="header.jsp" />

<h1>cPath2 Tests</h1>
<!-- basic html - just to show the test results -->
 <h2 id="qunit-banner"></h2>
 <div id="qunit-testrunner-toolbar"></div>
 <h2 id="qunit-userAgent"></h2>
 <ol id="qunit-tests"></ol>
 <div id="qunit-fixture">test markup, will be hidden</div>

<jsp:include page="footer.jsp" />
</body>
</html>
