FROM registry.cn-hangzhou.aliyuncs.com/bimface_common/centos7-openjdk11:v1
MAINTAINER zhaoyun <zhaoyun2316@gmail.com>

RUN yum install -y unzip

ENV VERSION 1.4.0

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

ADD apollo-portal-${VERSION}-github.zip /apollo-portal/apollo-portal-${VERSION}-github.zip

RUN unzip /apollo-portal/apollo-portal-${VERSION}-github.zip -d /apollo-portal \
    && rm -rf /apollo-portal/apollo-portal-${VERSION}-github.zip \
    && sed -i '$d' /apollo-portal/scripts/startup.sh \
    && echo "tail -f /opt/logs/100003173/apollo-portal.log" >> /apollo-portal/scripts/startup.sh

EXPOSE 8070

CMD ["/apollo-portal/scripts/startup.sh"]
