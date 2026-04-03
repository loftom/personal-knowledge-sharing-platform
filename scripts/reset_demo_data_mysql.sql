SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE badge_user;
TRUNCATE TABLE point_log;
TRUNCATE TABLE point_account;
TRUNCATE TABLE notification;
TRUNCATE TABLE follow_relation;
TRUNCATE TABLE user_behavior_event;
TRUNCATE TABLE qa_answer;
TRUNCATE TABLE qa_question;
TRUNCATE TABLE audit_log;
TRUNCATE TABLE favorite_record;
TRUNCATE TABLE like_record;
TRUNCATE TABLE comment;
TRUNCATE TABLE content_tag;
TRUNCATE TABLE content;
TRUNCATE TABLE user;
TRUNCATE TABLE sensitive_word;
TRUNCATE TABLE tag;
TRUNCATE TABLE category;

SET FOREIGN_KEY_CHECKS = 1;
