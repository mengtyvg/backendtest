    let items = [];
    let categories = [];
    let cart = [];
    let lastCompletedSale = null;
    let selectedCategory = "";
    let paymentMethods = [];
    let currentBakongPayment = null;
    let bakongPollTimer = null;
    let bakongExpiryTimer = null;
    let isSubmittingSale = false;
    let currentOrderDetailId = null;
    let currentOrderDetail = null;

    function showToast(type, title, message) {
      const container = document.getElementById("toastContainer");
      if (!container) return;

      const toast = document.createElement("div");
      toast.className = `toast ${type || "info"}`;
      toast.innerHTML = `
        <div class="toast-title">${escapeHtml(title || "Notice")}</div>
        ${message ? `<div class="toast-message">${escapeHtml(message)}</div>` : ""}
      `;
      container.appendChild(toast);

      setTimeout(() => {
        toast.classList.add("removing");
        toast.addEventListener("animationend", () => toast.remove(), { once: true });
      }, type === "error" ? 4200 : 3000);
    }

    function showSaleView() {
      document.querySelector(".main").style.display = "flex";
      document.getElementById("ordersPage").classList.remove("show");
      document.getElementById("salesNav").classList.add("active");
      document.getElementById("ordersNav").classList.remove("active");
    }

    function showOrdersView() {
      document.querySelector(".main").style.display = "none";
      document.getElementById("ordersPage").classList.add("show");
      document.getElementById("salesNav").classList.remove("active");
      document.getElementById("ordersNav").classList.add("active");
      loadTodayOrders();
    }

    async function loadTodayOrders() {
      const tbody = document.getElementById("ordersBody");
      const limit = Number(document.getElementById("invoiceLimit").value || 10);

      tbody.innerHTML = `
        <tr>
          <td colspan="4" class="orders-empty">Loading today's invoices...</td>
        </tr>
      `;

      try {
        const res = await fetch("/api/sale/orders/today");

        if (!res.ok) {
          throw new Error("Unable to load today's invoices");
        }

        const invoices = await res.json();
        renderOrders(invoices.slice(0, limit));
      } catch (error) {
        console.error(error);
        tbody.innerHTML = `
          <tr>
            <td colspan="4" class="orders-empty">${error.message}</td>
          </tr>
        `;
      }
    }

    function renderOrders(invoices) {
      const tbody = document.getElementById("ordersBody");

      tbody.innerHTML = invoices.map(inv => {
        const isVoided = inv.posOrderStatus === "Voided";
        const reason = inv.voidReason || "No reason saved";
        return `
        <tr class="${isVoided ? "voided-order" : ""}" onclick="openOrderDetail('${inv.id}')">
          <td>
            <div>${escapeHtml(inv.series || "-")}</div>
            ${isVoided ? `<div class="orders-void-reason">Reason: ${escapeHtml(reason)}</div>` : ""}
          </td>
          <td>${escapeHtml(inv.postDate || "-")}</td>
          <td><span class="orders-status ${isVoided ? "voided" : "completed"}">${escapeHtml(inv.posOrderStatus || "-")}</span></td>
          <td style="text-align:right;">$${Number(inv.total || 0).toFixed(2)}</td>
        </tr>
      `;
      }).join("") || `
        <tr>
          <td colspan="4" class="orders-empty">No invoices found for today.</td>
        </tr>
      `;
    }

    function formatDateTime(value) {
      if (!value) return "-";
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return String(value).replace("T", " ");
      }
      return date.toLocaleString();
    }

    function renderVoidDetails(detail) {
      const panel = document.getElementById("voidDetailPanel");
      const isVoided = detail.status === "Voided";
      panel.classList.toggle("show", isVoided);

      if (!isVoided) {
        document.getElementById("detailVoidReason").textContent = "-";
        document.getElementById("detailVoidedBy").textContent = "-";
        document.getElementById("detailVoidedAt").textContent = "-";
        document.getElementById("detailRefundAmount").textContent = "$0.00";
        return;
      }

      document.getElementById("detailVoidReason").textContent = detail.voidReason || detail.reason || "-";
      document.getElementById("detailVoidedBy").textContent = detail.voidedBy || "-";
      document.getElementById("detailVoidedAt").textContent = formatDateTime(detail.voidedAt);
      document.getElementById("detailRefundAmount").textContent = "$" + Number(detail.refundAmount || 0).toFixed(2);
    }

    async function openOrderDetail(invoiceId) {
      if (!invoiceId) return;

      const modal = document.getElementById("orderDetailModal");
      const detailItems = document.getElementById("detailItems");
      const voidButton = document.getElementById("voidInvoiceBtn");
      currentOrderDetailId = invoiceId;
      currentOrderDetail = null;

      document.getElementById("orderDetailSubtitle").textContent = "Loading invoice items...";
      document.getElementById("detailInvoiceNo").textContent = "-";
      document.getElementById("detailTotal").textContent = "$0.00";
      document.getElementById("detailStatus").textContent = "-";
      document.getElementById("detailItemCount").textContent = "0";
      renderVoidDetails({ status: "" });
      if (voidButton) {
        voidButton.disabled = true;
        voidButton.hidden = true;
        voidButton.textContent = "Void Invoice";
      }
      detailItems.innerHTML = `
        <div class="completed-item-row">
          <div class="completed-item-name">Loading items...</div>
        </div>
      `;
      modal.classList.add("show");

      try {
        const res = await fetch(`/api/sale/orders/${invoiceId}`);
        if (!res.ok) {
          throw new Error("Unable to load order detail");
        }

        const detail = await res.json();
        const items = Array.isArray(detail.items) ? detail.items : [];
        currentOrderDetail = detail;

        document.getElementById("orderDetailSubtitle").textContent =
          String(detail.dateCreated || detail.postDate || "-").replace("T", " ");
        document.getElementById("detailInvoiceNo").textContent = detail.series || "-";
        document.getElementById("detailTotal").textContent = "$" + Number(detail.total || 0).toFixed(2);
        document.getElementById("detailStatus").textContent = detail.status || "-";
        document.getElementById("detailItemCount").textContent = String(items.length);
        renderVoidDetails(detail);
        if (voidButton) {
          const canVoid = detail.status === "Completed";
          voidButton.hidden = !canVoid;
          voidButton.disabled = !canVoid;
        }

        detailItems.innerHTML = items.map(item => `
          <div class="completed-item-row">
            <div>
              <div class="completed-item-name">${escapeHtml(item.itemName || "-")}</div>
              <div class="completed-item-meta">
                ${escapeHtml(item.itemCode || item.sku || "-")} - ${Number(item.qty || 0)} x $${Number(item.unitPrice || 0).toFixed(2)}
              </div>
            </div>
            <div class="completed-item-total">$${Number(item.total || 0).toFixed(2)}</div>
          </div>
        `).join("") || `
          <div class="completed-item-row">
            <div class="completed-item-name">No items saved for this invoice.</div>
          </div>
        `;
      } catch (error) {
        console.error(error);
        closeOrderDetailModal();
        showToast("error", "Order Detail Failed", error.message || "Unable to load order detail");
      }
    }

    function closeOrderDetailModal() {
      document.getElementById("orderDetailModal").classList.remove("show");
      currentOrderDetailId = null;
      currentOrderDetail = null;
      closeVoidInvoiceModal();
    }

    function handleOrderDetailBackdropClick(event) {
      if (event.target.id === "orderDetailModal") {
        closeOrderDetailModal();
      }
    }

    function voidCurrentInvoice() {
      if (!currentOrderDetailId) {
        return;
      }

      const detail = currentOrderDetail || {};
      document.getElementById("voidInvoiceNo").textContent = detail.series || document.getElementById("detailInvoiceNo").textContent || "-";
      document.getElementById("voidRefundAmount").textContent = "$" + Number(detail.total || 0).toFixed(2);
      document.getElementById("voidReason").value = "";
      document.getElementById("voidReasonError").textContent = "";
      updateVoidReasonCount();
      document.getElementById("confirmVoidInvoiceBtn").disabled = false;
      document.getElementById("confirmVoidInvoiceBtn").textContent = "Void Invoice";
      document.getElementById("voidInvoiceModal").classList.add("show");
      setTimeout(() => document.getElementById("voidReason").focus(), 0);
    }

    function closeVoidInvoiceModal() {
      const modal = document.getElementById("voidInvoiceModal");
      if (modal) {
        modal.classList.remove("show");
      }
    }

    function handleVoidModalBackdropClick(event) {
      if (event.target.id === "voidInvoiceModal") {
        closeVoidInvoiceModal();
      }
    }

    function updateVoidReasonCount() {
      const reason = document.getElementById("voidReason");
      const count = document.getElementById("voidReasonCount");
      if (reason && count) {
        count.textContent = `${reason.value.length}/240`;
      }
    }

    async function confirmVoidInvoice() {
      const reasonInput = document.getElementById("voidReason");
      const reason = reasonInput.value.trim();
      const reasonError = document.getElementById("voidReasonError");
      if (!reason) {
        reasonError.textContent = "Reason is required.";
        reasonInput.focus();
        return;
      }

      const voidButton = document.getElementById("voidInvoiceBtn");
      const confirmButton = document.getElementById("confirmVoidInvoiceBtn");
      if (voidButton) {
        voidButton.disabled = true;
        voidButton.textContent = "Voiding...";
      }
      confirmButton.disabled = true;
      confirmButton.textContent = "Voiding...";

      try {
        const res = await fetch(`/api/sale/orders/${currentOrderDetailId}/void`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            voidReason: reason.trim(),
            voidedBy: "admin"
          })
        });

        if (!res.ok) {
          let message = "Unable to void invoice";
          try {
            const errorBody = await res.json();
            message = errorBody.detail || errorBody.message || message;
          } catch (_) {
            message = await res.text() || message;
          }
          throw new Error(message);
        }

        const result = await res.json();
        showToast("success", "Invoice Voided", result.message || "Invoice voided successfully.");
        closeVoidInvoiceModal();
        closeOrderDetailModal();
        await loadTodayOrders();
      } catch (error) {
        console.error(error);
        showToast("error", "Void Failed", error.message || "Unable to void invoice.");
        if (voidButton) {
          voidButton.disabled = false;
          voidButton.textContent = "Void Invoice";
        }
        confirmButton.disabled = false;
        confirmButton.textContent = "Void Invoice";
      }
    }



    async function loadItems() {
      try {
        const res = await fetch("/api/master/items-with-price");
        if (!res.ok) {
          throw new Error("Failed to load items");
        }

        items = await res.json();
        renderItems();
      } catch (error) {
        console.error(error);
        showToast("error", "Items Failed", "Unable to load items");
      }
    }

    async function loadPaymentMethods() {
      try {
        const res = await fetch("/api/sale/payment-methods");
        if (!res.ok) {
          throw new Error("Failed to load payment methods");
        }

        const data = await res.json();
        paymentMethods = Array.isArray(data) ? data : [];
        const select = document.getElementById("paymentMethod");
        select.innerHTML = `<option value="">Select Payment Method</option>`;

        paymentMethods.forEach(method => {
          select.innerHTML += `<option value="${escapeHtml(method.id)}">${escapeHtml(method.paymentSubtype)}</option>`;
        });
        renderPaymentMethodButtons();
      } catch (error) {
        console.error(error);
        showToast("error", "Payment Methods Failed", "Unable to load payment methods");
      }
    }

    function renderPaymentMethodButtons() {
      const wrap = document.getElementById("paymentMethodButtons");
      if (!wrap) return;

      const selectedId = document.getElementById("paymentMethod").value;
      wrap.innerHTML = paymentMethods.map(method => {
        const name = method.paymentSubtype || "-";
        const active = method.id === selectedId ? " active" : "";
        const khqr = isBakongPaymentMethod(name) ? " khqr" : "";
        return `
          <button class="payment-method-btn${active}${khqr}" type="button" onclick="selectPaymentMethod('${escapeHtml(method.id)}')">
            ${escapeHtml(name)}
          </button>
        `;
      }).join("");
    }

    function selectPaymentMethod(id) {
      document.getElementById("paymentMethod").value = id;
      renderPaymentMethodButtons();
    }

    async function loadCategories() {
      try {
        const res = await fetch("/api/master/categories");
        if (!res.ok) {
          throw new Error("Failed to load categories");
        }

        const data = await res.json();
        const seen = new Set();
        categories = data.filter(category => {
          const name = (category.name || "").trim();
          if (!name || seen.has(name)) {
            return false;
          }
          seen.add(name);
          return true;
        });
        renderCategoryTabs();
      } catch (error) {
        console.error(error);
        showToast("error", "Categories Failed", "Unable to load categories");
      }
    }

    function renderCategoryTabs() {
      const tabs = document.getElementById("categoryTabs");
      tabs.innerHTML = [
        `<button class="cat-btn ${selectedCategory === "" ? "active" : ""}" onclick="selectCategory('')">All</button>`,
        ...categories.map(category => `
          <button class="cat-btn ${selectedCategory === category.name ? "active" : ""}" onclick='selectCategory(${JSON.stringify(category.name)})'>
            ${category.name}
          </button>
        `)
      ].join("");
    }

    function selectCategory(categoryName) {
      selectedCategory = categoryName;
      renderCategoryTabs();
      renderItems();
    }

    function imgSrc(url) {
      if (!url) {
        return "https://placehold.co/300x180?text=No+Image";
      }
      return url;
    }

    function renderItems() {
      const keyword = document.getElementById("search").value.toLowerCase();
      const grid = document.getElementById("itemGrid");

      const filtered = [...items]
        .sort((a, b) => {
          const orderA = Number.isFinite(Number(a.displayOrder)) ? Number(a.displayOrder) : Number.MAX_SAFE_INTEGER;
          const orderB = Number.isFinite(Number(b.displayOrder)) ? Number(b.displayOrder) : Number.MAX_SAFE_INTEGER;

          if (orderA !== orderB) {
            return orderA - orderB;
          }

          return (a.itemName || "").localeCompare(b.itemName || "");
        })
        .filter(i =>
          (
            (i.itemName || "").toLowerCase().includes(keyword) ||
            (i.itemCode || "").toLowerCase().includes(keyword)
          ) &&
          (
            !selectedCategory ||
            (i.mengtyName || "") === selectedCategory
          )
        );

      grid.innerHTML = filtered.map(i => `
        <div class="item-card" onclick='addToCart(${JSON.stringify(i)})'>
          <img src="${imgSrc(i.imageUrl)}" alt="${i.itemName || "Product"}" onerror="this.src='https://placehold.co/300x180?text=No+Image'">
          <div class="iname">${i.itemName || "-"}</div>
          <div class="icategory">${i.mengtyName || "-"}</div>
          <div class="iprice">$${Number(i.itemPrice || 0).toFixed(2)}</div>
        </div>
      `).join("") || `<div style="padding:24px 16px;text-align:center;color:#9ca3af;font-size:12px">No items found</div>`;
    }

    function addToCart(item) {
      const found = cart.find(c => c.itemCode === item.itemCode);
      if (found) {
        found.qty++;
      } else {
        cart.push({
          itemCode: item.itemCode,
          itemVariantId: item.itemVariantId,
          itemName: item.itemName,
          mengtyName: item.mengtyName,
          imageUrl: item.imageUrl,
          price: Number(item.itemPrice || 0),
          qty: 1
        });
      }
      renderCart();
    }

    function renderCart() {
      const list = document.getElementById("cartList");

      list.innerHTML = cart.map(i => `
        <div class="order-row">
          <div class="rname">
            ${i.itemName}
            <span class="sub">${i.mengtyName || "-"}</span>
          </div>
          <div class="rprice">$${i.price.toFixed(2)}</div>
          <div class="rqty">
            <div class="qty-wrap">
              <button class="qty-btn" onclick="minusQty('${i.itemCode}')">-</button>
              <span>${i.qty}</span>
              <button class="qty-btn" onclick="plusQty('${i.itemCode}')">+</button>
            </div>
          </div>
          <div class="ramount">$${(i.price * i.qty).toFixed(2)}</div>
        </div>
      `).join("") || `<div style="padding:24px 16px;text-align:center;color:#9ca3af;font-size:12px">No items yet</div>`;

      updateTotals();
    }

    function plusQty(code) {
      const item = cart.find(i => i.itemCode === code);
      if (item) {
        item.qty++;
        renderCart();
      }
    }

    function minusQty(code) {
      const item = cart.find(i => i.itemCode === code);
      if (!item) {
        return;
      }

      item.qty--;
      if (item.qty <= 0) {
        cart = cart.filter(i => i.itemCode !== code);
      }
      renderCart();
    }

    function getTotal() {
      return cart.reduce((sum, i) => sum + (i.price * i.qty), 0);
    }

    function updateTotals() {
      const total = getTotal();
      const khr = Math.round(total * 4000);

      document.getElementById("subtotal").textContent = "$" + total.toFixed(2);
      document.getElementById("remain").textContent = "$" + total.toFixed(2);
      document.getElementById("total").textContent = total.toFixed(2);
      document.getElementById("totalKhr").textContent = khr.toLocaleString();
    }

    function escapeHtml(value) {
      return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
    }

    function printReceipt() {
      const paymentMethodName =
        lastCompletedSale?.paymentMethodName ||
        document.getElementById("paymentMethod").selectedOptions[0]?.text ||
        "Not selected";

      const sale = lastCompletedSale || {
        paymentMethodName,
        completedAt: new Date().toLocaleString(),
        total: getTotal(),
        items: cart.map(i => ({
          itemName: i.itemName,
          mengtyName: i.mengtyName,
          qty: i.qty,
          price: i.price,
          total: i.price * i.qty
        }))
      };

      if (!sale.items.length) {
        showToast("warning", "No Items", "There are no items to print.");
        return;
      }

      const receiptWindow = window.open("", "_blank", "width=420,height=720");
      receiptWindow.document.write(`
        <html>
        <head>
          <title>Receipt</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 16px; color: #111827; }
            h2 { margin: 0 0 10px; }
            p { margin: 4px 0; font-size: 12px; }
            .line { border-top: 1px dashed #9ca3af; margin: 12px 0; }
            .row { display: flex; justify-content: space-between; margin: 6px 0; font-size: 12px; gap: 8px; }
            .item-name { font-weight: 600; }
            .total { font-size: 14px; font-weight: 700; }
          </style>
        </head>
        <body>
          <h2>Mengty POS Receipt</h2>
          <p>${sale.completedAt}</p>

          <div class="line"></div>
          ${sale.items.map(i => `
            <div>
              <div class="item-name">${i.itemName || "-"}</div>
              <p>${i.mengtyName || "-"}</p>
              <div class="row">
                <span>$${i.price.toFixed(2)} x ${i.qty}</span>
                <span>$${i.total.toFixed(2)}</span>
              </div>
            </div>
          `).join("")}
          <div class="line"></div>
          <div class="row total">
            <span>Total</span>
            <span>$${sale.total.toFixed(2)}</span>
            <span>Payment Method: ${sale.paymentMethodName}</span>
          </div>
        </body>
        </html>
      `);
      receiptWindow.document.close();
      receiptWindow.focus();
      receiptWindow.print();
    }

    function showSaleCompleteModal(sale) {
      document.getElementById("completedAt").textContent = sale.completedAt;
      document.getElementById("completedInvoiceNo").textContent = sale.invoiceNo || "-";
      document.getElementById("completedTotal").textContent = "$" + Number(sale.total || 0).toFixed(2);
      document.getElementById("completedPayment").textContent = sale.paymentMethodName || "-";
      document.getElementById("completedItemCount").textContent = String(sale.items.length);

      document.getElementById("completedItems").innerHTML = sale.items.map(item => `
        <div class="completed-item-row">
          <div>
            <div class="completed-item-name">${escapeHtml(item.itemName || "-")}</div>
            <div class="completed-item-meta">
              ${escapeHtml(item.mengtyName || "-")} - ${Number(item.qty || 0)} x $${Number(item.price || 0).toFixed(2)}
            </div>
          </div>
          <div class="completed-item-total">$${Number(item.total || 0).toFixed(2)}</div>
        </div>
      `).join("");

      document.getElementById("saleCompleteModal").classList.add("show");
    }

    function closeSaleCompleteModal() {
      document.getElementById("saleCompleteModal").classList.remove("show");
    }

    function handleModalBackdropClick(event) {
      if (event.target.id === "saleCompleteModal") {
        closeSaleCompleteModal();
      }
    }

    function isBakongPaymentMethod(paymentMethodName) {
      const name = (paymentMethodName || "").toLowerCase();
      return name.includes("bakong") || name.includes("khqr") || name.includes("qr");
    }

    function setBakongStatus(text, type) {
      const status = document.getElementById("bakongPaymentStatus");
      status.textContent = text;
      status.className = `bakong-status ${type || ""}`.trim();
    }

    function stopBakongPolling() {
      if (bakongPollTimer) {
        clearInterval(bakongPollTimer);
        bakongPollTimer = null;
      }
      if (bakongExpiryTimer) {
        clearInterval(bakongExpiryTimer);
        bakongExpiryTimer = null;
      }
    }

    function setCompleteSaleLoading(loading, label) {
      const button = document.getElementById("completeSaleBtn");
      if (!button) return;
      button.disabled = loading;
      button.textContent = label || (loading ? "Processing..." : "Complete Sale");
    }

    function updateBakongExpiry() {
      if (!currentBakongPayment?.expiresAt) return;

      const expiresAt = new Date(currentBakongPayment.expiresAt).getTime();
      const remainingMs = expiresAt - Date.now();
      const expiry = document.getElementById("bakongPaymentExpiry");

      if (remainingMs <= 0) {
        expiry.textContent = "QR expired";
        setBakongStatus("Expired", "error");
        stopBakongPolling();
        return;
      }

      const totalSeconds = Math.ceil(remainingMs / 1000);
      const minutes = Math.floor(totalSeconds / 60);
      const seconds = String(totalSeconds % 60).padStart(2, "0");
      expiry.textContent = `Expires in ${minutes}:${seconds}`;
    }

    function openBakongPaymentModal(payment) {
      currentBakongPayment = payment;
      document.getElementById("bakongPaymentSubtitle").textContent = `Payment #${payment.paymentId}`;
      document.getElementById("bakongPaymentAmount").textContent =
        "$" + Number(payment.amount || getTotal()).toFixed(2);
      document.getElementById("bakongPaymentReference").textContent = `Payment reference #${payment.paymentId}`;
      document.getElementById("bakongQrImage").src =
        "https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=12&data=" +
        encodeURIComponent(payment.khqr || "");
      setBakongStatus("Pending", "");
      updateBakongExpiry();
      document.getElementById("bakongPaymentModal").classList.add("show");
      startBakongPolling();
    }

    function closeBakongPaymentModal() {
      stopBakongPolling();
      currentBakongPayment = null;
      setCompleteSaleLoading(false);
      document.getElementById("bakongPaymentModal").classList.remove("show");
    }

    function handleBakongBackdropClick(event) {
      if (event.target.id === "bakongPaymentModal") {
        closeBakongPaymentModal();
      }
    }

    function startBakongPolling() {
      stopBakongPolling();
      bakongPollTimer = setInterval(checkBakongPaymentNow, 3000);
      bakongExpiryTimer = setInterval(updateBakongExpiry, 1000);
    }

    async function startBakongPayment(paymentMethodName) {
      const missingVariant = cart.find(item => !item.itemVariantId);
      if (missingVariant) {
        showToast("error", "Payment Failed", `${missingVariant.itemName || "Item"} does not have a variant ID.`);
        return;
      }

      try {
        setCompleteSaleLoading(true, "Creating QR...");
        const res = await fetch("/api/payments", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            items: cart.map(item => ({
              itemVariantId: item.itemVariantId,
              qty: item.qty
            }))
          })
        });

        if (!res.ok) {
          let message = "Unable to create Bakong payment";
          try {
            const errorBody = await res.json();
            message = errorBody.detail || errorBody.message || message;
          } catch (_) {
            message = await res.text() || message;
          }
          throw new Error(message);
        }

        const payment = await res.json();
        payment.paymentMethodName = paymentMethodName;
        openBakongPaymentModal(payment);
      } catch (error) {
        console.error(error);
        setCompleteSaleLoading(false);
        showToast("error", "Bakong Failed", error.message || "Unable to create Bakong payment");
      }
    }

    async function checkBakongPaymentNow() {
      if (!currentBakongPayment?.paymentId) {
        return;
      }

      try {
        const res = await fetch(`/api/payments/${currentBakongPayment.paymentId}/check`);
        if (!res.ok) {
          throw new Error("Unable to check Bakong payment");
        }

        const result = await res.json();
        if (result.status === "PAID") {
          stopBakongPolling();
          setBakongStatus("Paid", "paid");
          document.getElementById("bakongPaymentSubtitle").textContent =
            result.bakongHash ? `Paid: ${result.bakongHash}` : "Payment received";
          await submitSale(currentBakongPayment.paymentMethodName);
          closeBakongPaymentModal();
          return;
        }

        if (result.status === "EXPIRED") {
          currentBakongPayment.expiresAt = result.expiresAt || currentBakongPayment.expiresAt;
          updateBakongExpiry();
          stopBakongPolling();
          setBakongStatus("Expired", "error");
          return;
        }

        setBakongStatus("Pending", "");
      } catch (error) {
        console.error(error);
        setBakongStatus("Check failed", "error");
      }
    }

    async function submitSale(paymentMethodName) {
      if (isSubmittingSale) {
        return;
      }

      isSubmittingSale = true;
      setCompleteSaleLoading(true, "Completing...");
      try {
        const paymentMethodId = document.getElementById("paymentMethod").value;
        const payload = {
          paymentMethodId,
          total: getTotal(),
          paymentId: currentBakongPayment?.paymentId || null,
          items: cart.map(i => ({
            itemCode: i.itemCode,
            itemName: i.itemName,
            unitPrice: i.price,
            qty: i.qty,
            total: i.price * i.qty
          }))
        };

        const res = await fetch("/api/sale/complete", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload)
        });

        if (!res.ok) {
          let message = "Sale failed";
          try {
            const errorBody = await res.json();
            message = errorBody.detail || errorBody.message || message;
          } catch (_) {
            message = await res.text() || message;
          }
          throw new Error(message);
        }

        const completed = await res.json();
        const completedItems = cart.map(i => ({
          itemName: i.itemName,
          mengtyName: i.mengtyName,
          qty: i.qty,
          price: i.price,
          total: i.price * i.qty
        }));

        lastCompletedSale = {
          invoiceNo: completed.series,
          paymentMethodName,
          completedAt: new Date().toLocaleString(),
          total: Number(completed.total || getTotal()),
          items: completedItems
        };

        showSaleCompleteModal(lastCompletedSale);
        cart = [];
        renderCart();
      } finally {
        isSubmittingSale = false;
        setCompleteSaleLoading(false);
      }
    }

    async function completeSale() {
      if (cart.length === 0) {
        showToast("warning", "Empty Cart", "Please add an item first.");
        return;
      }

      const paymentMethodId = document.getElementById("paymentMethod").value;
      const paymentMethodName = document.getElementById("paymentMethod").selectedOptions[0]?.text || "";

      if (!paymentMethodId) {
        showToast("warning", "Payment Required", "Please select a payment method.");
        return;
      }

      if (isBakongPaymentMethod(paymentMethodName)) {
        await startBakongPayment(paymentMethodName);
        return;
      }

      try {
        await submitSale(paymentMethodName);
      } catch (error) {
        console.error(error);
        showToast("error", "Sale Failed", error.message || "Sale failed");
      }
    }

    function setShiftInfo() {
      const now = new Date();
      const opts = { day: "numeric", month: "long" };
      document.getElementById("shiftInfo").textContent =
        now.toLocaleDateString("en-GB", opts) + " | Shift 1 | Cashier";
    }

    document.addEventListener("DOMContentLoaded", () => {
      loadItems();
      loadCategories();
      loadPaymentMethods();
      setShiftInfo();
      document.getElementById("invoiceLimit").addEventListener("change", loadTodayOrders);
      document.getElementById("paymentMethod").addEventListener("change", renderPaymentMethodButtons);
      document.getElementById("voidReason").addEventListener("input", () => {
        document.getElementById("voidReasonError").textContent = "";
        updateVoidReasonCount();
      });
      renderCart();
    });
