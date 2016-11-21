-- バッチ処理対象データを取得するSQL
GET_BATCH_INPUT_DATA =
SELECT
    BOOK_ID,
    TITLE,
    PUBLISHER,
    AUTHORS
FROM
    MESSAGING_BOOK
WHERE
    STATUS = '0'      -- 未処理のレコードが対象
ORDER BY
    TITLE

-- 処理ステータスを'1'(処理済み)に更新するSQL
UPDATE_STATUS_NORMAL_END =
UPDATE
    MESSAGING_BOOK
SET
    STATUS = '1'
WHERE
    BOOK_ID = :bookId

-- 処理ステータスを'2'(エラー)に更新するSQL
UPDATE_STATUS_ABNORMAL_END =
UPDATE
    MESSAGING_BOOK
SET
    STATUS = '2'
WHERE
    BOOK_ID = :bookId


