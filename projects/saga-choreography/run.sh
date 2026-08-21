#!/bin/bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home

echo "Starting Infrastructure..."
docker-compose up -d

echo "Starting Service Registry..."
nohup java -jar infra-service-registry/target/*.jar > registry.log 2>&1 &
PID_REGISTRY=$!
echo "Registry started with PID $PID_REGISTRY. Waiting 15s..."
sleep 15

echo "Starting Config Server..."
nohup java -jar infra-config-server/target/*.jar > config.log 2>&1 &
PID_CONFIG=$!
echo "Config Server started with PID $PID_CONFIG. Waiting 15s..."
sleep 15

echo "Starting API Gateway..."
nohup java -jar infra-api-gateway/target/*.jar > gateway.log 2>&1 &
PID_GATEWAY=$!
echo "API Gateway started with PID $PID_GATEWAY. Waiting 5s..."
sleep 5

echo "Starting Order Service..."
nohup java -jar order-service/target/*.jar > order.log 2>&1 &
PID_ORDER=$!
echo "Order Service started with PID $PID_ORDER"

echo "Starting Payment Service..."
nohup java -jar payment-service/target/*.jar > payment.log 2>&1 &
PID_PAYMENT=$!
echo "Payment Service started with PID $PID_PAYMENT"

echo "Starting Shipping Service..."
nohup java -jar shipping-service/target/*.jar > shipping.log 2>&1 &
PID_SHIPPING=$!
echo "Shipping Service started with PID $PID_SHIPPING"

echo "All services started."
echo "To stop them, run: kill $PID_REGISTRY $PID_CONFIG $PID_GATEWAY $PID_ORDER $PID_PAYMENT $PID_SHIPPING"
