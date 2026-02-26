alter table transactions
    drop constraint if exists fk_transactions_category_owner;

alter table transactions
    add constraint fk_transactions_category
    foreign key (category_id)
    references categories(id)
    on delete set null;
