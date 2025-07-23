#!/bin/bash

echo "=== Testing Different Publishers for Outbox Pattern ==="
echo ""

# Function to test a publisher
test_publisher() {
    local publisher_type=$1
    local port=$2
    echo "🔄 Testing $publisher_type publisher..."
    echo "Starting services..."
    
    # Start infrastructure services
    docker-compose up -d mysql kafka rabbitmq &>/dev/null
    
    # Wait for services to be ready
    sleep 5
    
    echo "Starting application with $publisher_type publisher..."
    
    # Start app in background with specific publisher
    OUTBOX_PUBLISHER_TYPE=$publisher_type SERVER_PORT=$port ./gradlew bootRun --no-daemon > "logs/app-$publisher_type.log" 2>&1 &
    APP_PID=$!
    
    # Wait for app to start
    echo "Waiting for application to start on port $port..."
    sleep 10
    
    # Test if app is running
    if ! curl -s "http://localhost:$port/actuator/health" > /dev/null; then
        echo "❌ Application failed to start"
        kill $APP_PID 2>/dev/null
        return 1
    fi
    
    echo "✅ Application started successfully"
    
    # Test publisher info
    echo "Publisher info:"
    curl -s "http://localhost:$port/api/demo/publisher-info" | jq '.' 2>/dev/null || echo "Failed to get publisher info"
    
    # Create test message
    echo "Creating test message..."
    response=$(curl -s -X POST "http://localhost:$port/api/demo/unordered-message?topic=test-topic-$publisher_type&message=Hello from $publisher_type")
    echo "Response: $response"
    
    # Process messages
    echo "Processing pending messages..."
    curl -s -X POST "http://localhost:$port/api/demo/process-pending" | jq '.' 2>/dev/null || echo "Processing completed"
    
    # Get metrics
    echo "Current metrics:"
    curl -s "http://localhost:$port/api/demo/metrics" | jq '.' 2>/dev/null || echo "Failed to get metrics"
    
    echo "✅ $publisher_type test completed"
    echo "📋 Check logs/app-$publisher_type.log for detailed logs"
    
    # Cleanup
    kill $APP_PID 2>/dev/null
    sleep 2
    
    echo ""
}

# Create logs directory
mkdir -p logs

echo "Starting infrastructure services..."
docker-compose up -d mysql kafka rabbitmq
echo "Waiting for services to be ready..."
sleep 10

# Test each publisher
test_publisher "logging" "8080"
test_publisher "kafka" "8081" 
test_publisher "rabbitmq" "8082"

echo "=== All Publisher Tests Completed ==="
echo ""
echo "Summary:"
echo "📝 Logging Publisher: Messages logged to console"
echo "🔄 Kafka Publisher: Messages sent to Kafka (check Kafka UI at http://localhost:8081)"
echo "🐰 RabbitMQ Publisher: Messages sent to RabbitMQ (check Management UI at http://localhost:15672 - admin/admin)"
echo ""
echo "Docker services are still running. To stop them:"
echo "  docker-compose down"
echo ""
echo "Log files:"
echo "  logs/app-logging.log"
echo "  logs/app-kafka.log"
echo "  logs/app-rabbitmq.log" 