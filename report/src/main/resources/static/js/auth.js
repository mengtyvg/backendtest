const FRONTEND_ROLE_PAGES = {
  ADMIN: ["/sale.html", "/payment-history.html", "/item.html", "/category.html", "/stock.html", "/payment-method.html", "/report.html", "/salesummary.html", "/users.html"],
  MANAGER: ["/sale.html", "/payment-history.html", "/item.html", "/category.html", "/stock.html", "/payment-method.html", "/report.html", "/salesummary.html"],
  CASHIER: ["/sale.html", "/payment-history.html"]
};

const FRONTEND_ROLE_HOME = {
  ADMIN: "/item.html",
  MANAGER: "/stock.html",
  CASHIER: "/sale.html"
};

function getCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem("currentUser"));
  } catch {
    return null;
  }
}

function getAccessToken() {
  return localStorage.getItem("accessToken") || "";
}

function isAccessTokenExpired(token = getAccessToken()) {
  if (!token) return true;

  try {
    const payload = token.split(".")[1]
      .replace(/-/g, "+")
      .replace(/_/g, "/");
    const decoded = JSON.parse(atob(payload));
    return !decoded.exp || decoded.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

function getCurrentRole(user = getCurrentUser()) {
  return String(user?.role || "").trim().toUpperCase();
}

function getAllowedPages(user = getCurrentUser()) {
  return FRONTEND_ROLE_PAGES[getCurrentRole(user)] || [];
}

function getDefaultPage(user = getCurrentUser()) {
  const role = getCurrentRole(user);
  const allowedPages = getAllowedPages(user);
  const requestedPage = String(user?.defaultPage || "").trim();

  if (requestedPage && allowedPages.includes(requestedPage)) {
    return requestedPage;
  }

  return FRONTEND_ROLE_HOME[role] || "/login.html";
}

function clearFrontendSession() {
  localStorage.removeItem("isLoggedIn");
  localStorage.removeItem("currentUser");
  localStorage.removeItem("accessToken");
  localStorage.removeItem("accessTokenExpiresAt");
}

function logout() {
  clearFrontendSession();
  window.location.replace("/login.html");
}

function requirePageAccess() {
  const user = getCurrentUser();
  const isLoggedIn = localStorage.getItem("isLoggedIn") === "true";
  const accessToken = getAccessToken();

  if (!isLoggedIn || !user || !accessToken || isAccessTokenExpired(accessToken)) {
    clearFrontendSession();
    window.location.replace("/login.html");
    return false;
  }

  const path = window.location.pathname;
  if (!getAllowedPages(user).includes(path)) {
    window.location.replace(getDefaultPage(user));
    return false;
  }

  return true;
}

const originalFetch = window.fetch.bind(window);
window.fetch = async function authenticatedFetch(input, init = {}) {
  const url = typeof input === "string" ? input : input.url;
  const isApiRequest = new URL(url, window.location.origin).pathname.startsWith("/api/");
  const isLoginRequest = new URL(url, window.location.origin).pathname === "/api/users/login";
  const token = getAccessToken();
  const headers = new Headers(init.headers || (input instanceof Request ? input.headers : undefined));

  if (isApiRequest && !isLoginRequest && token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await originalFetch(input, { ...init, headers });
  if (isApiRequest && !isLoginRequest && response.status === 401) {
    clearFrontendSession();
    if (window.location.pathname !== "/login.html") {
      window.location.replace("/login.html");
    }
  }

  return response;
};

function redirectAfterLogin(user) {
  window.location.href = getDefaultPage(user);
}

function hideForbiddenLinks() {
  const allowedPages = getAllowedPages();

  document.querySelectorAll("a[href]").forEach(link => {
    const href = link.getAttribute("href");
    if (href && FRONTEND_ROLE_PAGES.ADMIN.includes(href)) {
      const forbidden = !allowedPages.includes(href);
      link.hidden = forbidden;
      link.style.display = forbidden ? "none" : "";
    }
  });
}

document.addEventListener("DOMContentLoaded", hideForbiddenLinks);
