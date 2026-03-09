create or replace function core.generate_random_string(length int)
    returns varchar
    language sql
as
$$
SELECT array_to_string(array(select substr('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789',
                                           ((random() * (36 - 1) + 1)::integer), 1)
                             from generate_series(1, length)), '');
$$;