CREATE TABLE ts.metrics (
metric_name text,
day_bucket int,
tag_hash text,
time timeuuid,
value float,
PRIMARY KEY ((metric_name, day_bucket), tag_hash, time)
) WITH CLUSTERING ORDER BY (tag_hash ASC, time DESC)
AND bloom_filter_fp_chance = 0.01
AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
AND comment = ''
AND compaction = {'class': 'TimeWindowCompactionStrategy', 'compaction_window_size': '2', 'compaction_window_unit': 'DAYS'}
AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
AND crc_check_chance = 1.0
AND default_time_to_live = 0
AND gc_grace_seconds = 864000
AND max_index_interval = 2048
AND memtable_flush_period_in_ms = 0
AND min_index_interval = 128
AND speculative_retry = '99PERCENTILE';

