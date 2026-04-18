-- Populate dim_date with dates from 2020 to 2030
-- Run this after creating the schema

INSERT INTO blood_ops.dim_date
SELECT
    toUInt32(toYYYYMMDD(dt)) AS date_id,
    dt,

    -- Year attributes
    toYear(dt) AS year,
    concat('Year ', toString(toYear(dt))) AS year_name,

    -- Quarter attributes
    toQuarter(dt) AS quarter,
    concat('Q', toString(toQuarter(dt)), ' ', toString(toYear(dt))) AS quarter_name,

    -- Month attributes
    toMonth(dt) AS month,
    dateName('month', dt) AS month_name,
    substring(dateName('month', dt), 1, 3) AS month_short_name,

    -- Week attributes
    toISOWeek(dt) AS iso_week,
    toUInt8(ceil(toDayOfMonth(dt) / 7.0)) AS week_of_month,
    toMonday(dt) AS week_start_date,
    addDays(toMonday(dt), 6) AS week_end_date,

    -- Day attributes
    toDayOfMonth(dt) AS day,
    toDayOfWeek(dt) AS day_of_week,
    dateName('weekday', dt) AS day_of_week_name,
    substring(dateName('weekday', dt), 1, 3) AS day_of_week_short,
    toDayOfYear(dt) AS day_of_year,

    -- Flags
    if(toDayOfWeek(dt) IN (6, 7), 1, 0) AS is_weekend,
    if(toDayOfMonth(dt) = 1, 1, 0) AS is_month_start,
    if(toDayOfMonth(addDays(dt, 1)) = 1, 1, 0) AS is_month_end,
    if(toDayOfMonth(dt) = 1 AND toMonth(dt) IN (1, 4, 7, 10), 1, 0) AS is_quarter_start,
    if(toDayOfMonth(addDays(dt, 1)) = 1 AND toMonth(addDays(dt, 1)) IN (1, 4, 7, 10), 1, 0) AS is_quarter_end,
    if(toDayOfYear(dt) = 1, 1, 0) AS is_year_start,
    if(toDayOfYear(addDays(dt, 1)) = 1, 1, 0) AS is_year_end,

    -- Relative periods
    toInt32(toDays(dt)) AS days_from_epoch,
    toUInt32(toYear(dt) * 100 + toISOWeek(dt)) AS week_id,
    toUInt32(toYear(dt) * 100 + toMonth(dt)) AS month_id,
    toUInt32(toYear(dt) * 10 + toQuarter(dt)) AS quarter_id
FROM
(
    SELECT
        toDate('2020-01-01') + number AS dt
    FROM numbers(3653)  -- 2020-01-01 to 2030-01-01 (10+ years)
    WHERE dt >= '2020-01-01' AND dt <= '2030-12-31'
);
