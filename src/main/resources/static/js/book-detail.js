document.addEventListener("DOMContentLoaded", () => {

    const exchangeButton =
        document.querySelector("#exchangeButton");

    if (!exchangeButton) {
        return;
    }

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

            if (response.status === 401) {
                alert("로그인이 필요합니다. 다시 로그인해주세요.");
                window.location.href = "/login";
                return;
            }

            if (!response.ok) {
                const message = await response.text();
                throw new Error(message || "교환 요청에 실패했습니다.");
            }

            alert("교환 요청이 완료되었습니다.");

            location.reload();

        } catch (error) {

            alert(error.message);

        }

    });

});

async function resetCache(bookId) {

    try {
        const response = await fetch(`/books/${bookId}/cache/reset`, {
            method: "POST"
        });

        if (!response.ok) {
            alert("캐시 초기화에 실패했습니다.");
            return;
        }

        alert(
            "Redis 캐시를 초기화했습니다.\n\n" +
            "현재 도서의 상세 캐시가 삭제되었습니다.\n" +
            "이제 페이지를 다시 조회하여 MISS 과정을 확인합니다."
        );

        // 다시 상세 페이지를 요청
        // Redis에 캐시가 없으므로 MISS 발생
        location.reload();

    } catch (error) {

        console.error("Redis 캐시 초기화 실패:", error);

        alert("캐시 초기화 중 오류가 발생했습니다.");
    }
}