document.addEventListener("DOMContentLoaded", () => {

    // 도서 설명 글자 수 표시
    const description = document.querySelector("#description");
    const descriptionCount = document.querySelector("#descriptionCount");

    if (description && descriptionCount) {
        description.addEventListener("input", () => {
            descriptionCount.textContent = description.value.length;
        });
    }


    // 도서 표지 이미지 업로드
    const bookImageInput = document.querySelector("#bookImage");

    if (bookImageInput && typeof FilePond !== "undefined") {

        FilePond.registerPlugin(
            FilePondPluginFileValidateType,
            FilePondPluginFileValidateSize,
            FilePondPluginImagePreview
        );

        FilePond.create(bookImageInput, {

            acceptedFileTypes: [
                "image/png",
                "image/jpeg",
                "image/webp"
            ],

            maxFileSize: "5MB",

            allowMultiple: false,

            // 별도 FilePond 서버 업로드가 아니라
            // 기존 HTML form과 함께 전송
            storeAsFile: true,

            labelIdle:
                '도서 표지를 드래그하거나 <span class="filepond--label-action">파일 선택</span>',

            labelFileTypeNotAllowed:
                "이미지 파일만 등록할 수 있습니다.",

            fileValidateTypeLabelExpectedTypes:
                "PNG, JPG, JPEG, WebP 이미지를 선택해주세요.",

            labelMaxFileSizeExceeded:
                "파일 크기가 너무 큽니다.",

            labelMaxFileSize:
                "최대 파일 크기는 5MB입니다."
        });
    }

});