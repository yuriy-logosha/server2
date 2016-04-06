User cron:

*/5 * * * * cd /home/ubuntu/sslv; mvn test -Dscope=today -Dthreads=10
/usr/bin/java -Xms256m -Xmx1g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:+HeapDumpOnOutOfMemoryError -XX:+DisableExplicitGC -cp sslv-0.0.1-SNAPSHOT.jar com.sslv.SSLVLauncher start -d -p /var/run/sslvlauncher/sslvlauncher.pid

Root crons:
*/10 * * * * sudo service kibana start >> /home/ubuntu/kibana-cron-log
* */2 * * * sudo service kibana restart >> /home/ubuntu/kibana-cron-restart-log
*/1 * * * * sudo service elasticsearch start  >> /home/ubuntu/elasticsearch-cron-log
