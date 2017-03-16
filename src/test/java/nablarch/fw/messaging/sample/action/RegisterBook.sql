REGISTER_BOOK = INSERT INTO BOOK_DATA (
                    TITLE, PUBLISHER, AUTHORS
                ) VALUES (
                    :title, :publisher, :authors
                )

INSERT_ERROR_LOG =
INSERT INTO ERROR_LOG
(ERROR_MESSAGE)
VALUES
(?)

