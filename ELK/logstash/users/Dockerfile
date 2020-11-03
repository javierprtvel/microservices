FROM logstash:7.9.1
RUN rm -f /usr/share/logstash/pipeline/logstash.conf
COPY logstash.conf /usr/share/logstash/pipeline/logstash.conf
COPY logstash.yml config/logstash.yml