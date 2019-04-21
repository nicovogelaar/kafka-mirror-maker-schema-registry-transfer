FROM confluentinc/cp-kafka:5.1.0

ENV COMPONENT=kafka-mirror-maker

RUN mkdir -p /etc/"${COMPONENT}"
COPY include/etc/giogt/docker /etc/giogt/docker

CMD ["/etc/giogt/docker/run"]
