# My Steps

## Setup
1. Get docker image with `docker pull qpswwww/quickstarts:v2`
2. Copy cloned project in Programmieren / Intellij folder
3. Cd in the project folder, then: `export localdir="$PWD"`, then:
    ```
    docker run --hostname=quickstart.cloudera --privileged=true -t -i \
      -p 8888:8888 -p 10000:10000 -p 10020:10020 -p 11000:11000 \
      -p 18080:18080 -p 18081:18081 -p 18088:18088 -p 19888:19888 \
      -p 21000:21000 -p 21050:21050 -p 2181:2181 \
      -p 25000:25000 -p 25010:25010 -p 25020:25020 \
      -p 50010:50010 -p 50030:50030 -p 50060:50060 -p 50070:50070 -p 50075:50075 -p 50090:50090 \
      -p 60000:60000 -p 60010:60010 -p 60020:60020 -p 60030:60030 \
      -p 7180:7180 -p 7183:7183 -p 7187:7187 \
      -p 80:80 -p 8020:8020 -p 8032:8032 -p 8042:8042 -p 8088:8088 -p 8983:8983 -p 9083:9083 \
      -v "$localdir":/host -m=8g qpswwww/quickstarts:v2 /etc/bootstrap.sh -bash
    ```
4. Now do:
    ```
    source /etc/environment   # set up Hadoop env vars
    cd /host                  # this is your Mac’s assignment directory
    ```
   
## Warming up
In1: `hadoop fs -put 1400-8.txt`
Out1: ```put: `1400-8.txt': File exists```

In2: `hadoop jar target/assignment-3-1.0-SNAPSHOT.jar hk.ust.comp4651.WordCount -input 1400-8.txt -output wc -numReducers 2`
```
25/10/26 11:47:29 INFO comp4651.WordCount: Tool: WordCount
25/10/26 11:47:29 INFO comp4651.WordCount:  - input path: 1400-8.txt
25/10/26 11:47:29 INFO comp4651.WordCount:  - output path: wc
25/10/26 11:47:29 INFO comp4651.WordCount:  - number of reducers: 2
25/10/26 11:47:30 INFO client.RMProxy: Connecting to ResourceManager at /0.0.0.0:8032
25/10/26 11:47:31 INFO input.FileInputFormat: Total input paths to process : 1
25/10/26 11:47:31 INFO mapreduce.JobSubmitter: number of splits:1
25/10/26 11:47:31 INFO mapreduce.JobSubmitter: Submitting tokens for job: job_1761493512717_0001
25/10/26 11:47:32 INFO impl.YarnClientImpl: Submitted application application_1761493512717_0001
25/10/26 11:47:32 INFO mapreduce.Job: The url to track the job: http://quickstart.cloudera:8088/proxy/application_1761493512717_0001/
25/10/26 11:47:32 INFO mapreduce.Job: Running job: job_1761493512717_0001
25/10/26 11:47:38 INFO mapreduce.Job: Job job_1761493512717_0001 running in uber mode : false
25/10/26 11:47:38 INFO mapreduce.Job:  map 0% reduce 0%
25/10/26 11:47:44 INFO mapreduce.Job:  map 100% reduce 0%
25/10/26 11:47:50 INFO mapreduce.Job:  map 100% reduce 100%
25/10/26 11:47:51 INFO mapreduce.Job: Job job_1761493512717_0001 completed successfully
25/10/26 11:47:51 INFO mapreduce.Job: Counters: 49
        File System Counters
                FILE: Number of bytes read=325687
                FILE: Number of bytes written=996792
                FILE: Number of read operations=0
                FILE: Number of large read operations=0
                FILE: Number of write operations=0
                HDFS: Number of bytes read=1033868
                HDFS: Number of bytes written=238960
                HDFS: Number of read operations=9
                HDFS: Number of large read operations=0
                HDFS: Number of write operations=4
        Job Counters 
                Launched map tasks=1
                Launched reduce tasks=2
                Data-local map tasks=1
                Total time spent by all maps in occupied slots (ms)=3457
                Total time spent by all reduces in occupied slots (ms)=6616
                Total time spent by all map tasks (ms)=3457
                Total time spent by all reduce tasks (ms)=6616
                Total vcore-seconds taken by all map tasks=3457
                Total vcore-seconds taken by all reduce tasks=6616
                Total megabyte-seconds taken by all map tasks=3539968
                Total megabyte-seconds taken by all reduce tasks=6774784
        Map-Reduce Framework
                Map input records=20409
                Map output records=187462
                Map output bytes=1758828
                Map output materialized bytes=325687
                Input split bytes=117
                Combine input records=187462
                Combine output records=22183
                Reduce input groups=22183
                Reduce shuffle bytes=325687
                Reduce input records=22183
                Reduce output records=22183
                Spilled Records=44366
                Shuffled Maps =2
                Failed Shuffles=0
                Merged Map outputs=2
                GC time elapsed (ms)=86
                CPU time spent (ms)=5310
                Physical memory (bytes) snapshot=630300672
                Virtual memory (bytes) snapshot=2250260480
                Total committed heap usage (bytes)=507510784
        Shuffle Errors
                BAD_ID=0
                CONNECTION=0
                IO_ERROR=0
                WRONG_LENGTH=0
                WRONG_MAP=0
                WRONG_REDUCE=0
        File Input Format Counters 
                Bytes Read=1033751
        File Output Format Counters 
                Bytes Written=238960
25/10/26 11:47:51 INFO comp4651.WordCount: Job Finished in 21.231 seconds
```

In3: `hadoop fs -ls wc`
Out3:
```
Found 3 items
-rw-r--r--   1 root supergroup          0 2025-10-26 11:47 wc/_SUCCESS
-rw-r--r--   1 root supergroup     119532 2025-10-26 11:47 wc/part-r-00000
-rw-r--r--   1 root supergroup     119428 2025-10-26 11:47 wc/part-r-00001
```

In4: `hadoop jar target/assignment-3-1.0-SNAPSHOT.jar hk.ust.comp4651.AnalyzeWordCount -input wc`
Out4:
```
total number of unique words: 22183
total number of words: 187462
number of words that appear only once: 13167

ten most frequent words: 
the     7885
and     6509
I       5670
to      5018
of      4446
a       3961
in      2847
was     2685
that    2648
had     2056
```

In5:
