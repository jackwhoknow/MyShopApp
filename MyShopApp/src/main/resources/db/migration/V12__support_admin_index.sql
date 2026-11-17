-- 平台客服：全部会话按最后消息时间倒序分页，补充复合索引避免全表排序
create index idx_support_conversation_last_message
    on support_conversation (last_message_at, id);
