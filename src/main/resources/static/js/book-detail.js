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
                throw new Error("교환 요청에 실패했습니다.");
            }

            alert("교환 요청이 완료되었습니다.");

            location.reload();

        } catch (error) {

            alert(error.message);

        }

    });

});