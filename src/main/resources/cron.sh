User cron:

*/5 * * * * /usr/bin/java -Dscope=today -Dthreads=10 -Xmx32M -Xms5m -XX:+UseParNewGC -XX:-UseGCOverheadLimit -jar /home/ubuntu/sslv/target/sslv-0.0.1-SNAPSHOT-jar-with-dependencies.jar start -d -p /var/run/sslvlauncher/sslvlauncher.pid

Root crons:
*/10 * * * * sudo service kibana start >> /home/ubuntu/kibana-cron-log
* */2 * * * sudo service kibana restart >> /home/ubuntu/kibana-cron-restart-log
*/1 * * * * sudo service elasticsearch start  >> /home/ubuntu/elasticsearch-cron-log
