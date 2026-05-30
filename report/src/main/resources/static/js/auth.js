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
}

function logout() {
  clearFrontendSession();
  window.location.replace("/login.html");
}

function requirePageAccess() {
  const user = getCurrentUser();
  const isLoggedIn = localStorage.getItem("isLoggedIn") === "true";

  if (!isLoggedIn || !user) {
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
