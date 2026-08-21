#!/bin/bash
CUSTOMER_ID=$(uuidgen)
PRODUCT_ID=$(uuidgen)

echo "Creating Order for Customer: $CUSTOMER_ID, Product: $PRODUCT_ID"
RESPONSE=$(curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d "{\"customerId\": \"$CUSTOMER_ID\", \"productId\": \"$PRODUCT_ID\", \"price\": 100.0}")

echo "Create Response: $RESPONSE"

ORDER_ID=$(echo $RESPONSE | grep -o '"id":[^,]*' | awk -F: '{print $2}')

if [ -z "$ORDER_ID" ]; then
  echo "Failed to create order"
  exit 1
fi

echo "Order ID: $ORDER_ID"
echo "Waiting for Saga to complete (5s)..."
sleep 5

# Note: We don't have a GET endpoint for order by ID in the controller yet, 
# so we can't verify the final status via API unless we add it.
# But we can check the logs (which are being tailed in the run script output if running in foreground, or in log files).
# For now, we assume if create worked, the flow started.

# Let's check the order log for "Order Completed" or similar if possible, 
# but since we are in a script, we can't easily grep the background log file unless we know where it is.
# The run.sh puts logs in order.log.

echo "Checking Order Service logs for completion..."
grep "Order Completed" order.log
if [ $? -eq 0 ]; then
  echo "SUCCESS: Order flow completed!"
else
  echo "WARNING: 'Order Completed' not found in logs yet. Check order.log, payment.log, shipping.log."
fi
