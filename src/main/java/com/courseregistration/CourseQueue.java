package com.courseregistration;


    public class CourseQueue {
        private Request[] queueArray;
        private int front;
        private int rear;
        private int currentSize;
        private int capacity;

        public CourseQueue(int size) {
            this.capacity = size;
            this.queueArray = new Request[capacity];
            this.front = 0;
            this.rear = -1;
            this.currentSize = 0;
        }

        // Your main responsibility: Adding requests to the line
        public boolean enqueue(Request req) {
            if (currentSize == capacity) {
                System.out.println("Error: Queue Full");
                return false; // Rejects the request if the queue is at maximum capacity
            }

            // Circular queue logic: Wraps around to the start of the array if there is space
            rear = (rear + 1) % capacity;
            queueArray[rear] = req;
            currentSize++;
            return true;
        }

        // Member 2's responsibility: Pulling requests out of the line (needed for later)
        public Request dequeue() {
            if (currentSize == 0) return null;
            Request temp = queueArray[front];
            front = (front + 1) % capacity;
            currentSize--;
            return temp;
        }
    }

