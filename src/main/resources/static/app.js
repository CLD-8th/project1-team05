document.addEventListener("DOMContentLoaded", () => {

    const searchInput = document.querySelector("#bookSearch");
    const statusFilter = document.querySelector("#statusFilter");
    const books = document.querySelectorAll("#bookList .book-preview");
    const emptyResult = document.querySelector("#emptyResult");
    const description = document.querySelector("#description");
    const descriptionCount = document.querySelector("#descriptionCount");
    const exchangeButton = document.querySelector("#exchangeButton");

    function filterBooks() {

        if (!searchInput || !statusFilter) {
            return;
        }

        const keyword = searchInput.value.toLowerCase().trim();
        const selectedStatus = statusFilter.value;

        let visibleCount = 0;

        books.forEach(book => {

            const title = (book.dataset.title || "").toLowerCase();
            const author = (book.dataset.author || "").toLowerCase();
            const status = book.dataset.status;

            const matchesKeyword =
                title.includes(keyword) ||
                author.includes(keyword);

            const matchesStatus =
                selectedStatus === "ALL" ||
                status === selectedStatus;

            if (matchesKeyword && matchesStatus) {
                book.style.display = "";
                visibleCount++;
            } else {
                book.style.display = "none";
            }
        });

        if (emptyResult) {
            emptyResult.style.display =
                visibleCount === 0 ? "block" : "none";
        }
    }

    if (searchInput) {
        searchInput.addEventListener("input", filterBooks);
    }

    if (statusFilter) {
        statusFilter.addEventListener("change", filterBooks);
    }

    if (description && descriptionCount) {

        description.addEventListener("input", () => {
            descriptionCount.textContent = description.value.length;
        });

    }

    if (exchangeButton) {

        exchangeButton.addEventListener("click", async () => {

            const bookId = exchangeButton.dataset.bookId;

            const confirmed = confirm(
                "이 도서에 교환 요청을 보내시겠습니까?"
            );

            if (!confirmed) {
                return;
            }

            try {

                const response = await fetch(
                    `/books/${bookId}/exchange`,
                    {
                        method: "POST"
                    }
                );

                if (!response.ok) {
                    throw new Error("교환 요청에 실패했습니다.");
                }

                alert("교환 요청이 완료되었습니다.");

                location.reload();

            } catch (error) {

                alert(error.message);

            }

        });

    }

});