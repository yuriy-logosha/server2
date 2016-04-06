User cron:

*/5 * * * * cd /home/ubuntu/sslv; mvn test -Dscope=today -Dthreads=10

Root crons:
*/10 * * * * sudo service kibana start >> /home/ubuntu/kibana-cron-log
* */2 * * * sudo service kibana restart >> /home/ubuntu/kibana-cron-restart-log
*/1 * * * * sudo service elasticsearch start  >> /home/ubuntu/elasticsearch-cron-log
