CREATE TABLE ts.tags2 (
metric_name text,
tag_hash text,
tags map<text, text>,
PRIMARY KEY ((metric_name), tag_hash)
) WITH CLUSTERING ORDER BY (tag_hash ASC)
AND bloom_filter_fp_chance = 0.01
AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
AND comment = ''
AND compaction = {
    'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy',
    'max_threshold': '32', 'min_threshold': '4'
    }
AND compression = {
    'chunk_length_in_kb': '64',
    'class': 'org.apache.cassandra.io.compress.LZ4Compressor'
    }
AND crc_check_chance = 1.0
AND default_time_to_live = 0
AND gc_grace_seconds = 864000
AND max_index_interval = 2048
AND memtable_flush_period_in_ms = 0
AND min_index_interval = 128
AND speculative_retry = '99PERCENTILE';

CREATE INDEX ix_tags2__tags ON ts.tags2 (ENTRIES(tags));

