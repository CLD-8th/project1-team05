document.addEventListener("DOMContentLoaded", () => {

    const searchInput = document.querySelector("#bookSearch");
    const statusFilter = document.querySelector("#statusFilter");
    const books = document.querySelectorAll("#bookList .book-preview");
    const emptyResult = document.querySelector("#emptyResult");

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

    searchInput?.addEventListener("input", filterBooks);
    statusFilter?.addEventListener("change", filterBooks);

});