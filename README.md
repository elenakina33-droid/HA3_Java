## Project Overview
This project simulates a fully automated smart storage system with:
- Parallel charging of AGVs
- Task management and autonomous dispatching
- Simulation of multiple tasks running simultaneously using concurrency

The system models real medical warehouse behavior using multithreading, parallel execution, and automatic resource allocation.

---

## Team Work Distribution
### Team mate(Kristian) – Focused on Requirement 1**
**Parallel Charging Simulation**  
Implemented:
- Charging multiple AGVs simultaneously (1..N)
- Concurrency using thread pools for charging stations
- Random arrival simulation for AGVs
- Queue management for AGVs waiting to charge
- Logging of all charging events

This includes:
- Detecting free charging stations
- Running each charging process in a separate thread
- Ensuring real-time behavior and no UI blocking

---

### **Me(Elena) – Focused on Requirement 2**
**Random Arrival & Queue Management for Charging**

Implemented:
- Random arrival generation for AGVs
- Queue logic and waiting list simulation
- Waiting time calculation based on current charging conditions
- Queue timeout logic (AGVs leave the queue if projected wait > 15 minutes)
- Logging all queue-related events

This work directly complements requirement 1 and ensures realistic simulation of resource contention.

---

### **Both Team Members – Requirement 3**
**Parallel Task Execution with K Available AGVs**

This part was implemented collaboratively:
- Support for running M tasks in parallel
- Assigning tasks to K AGVs using concurrency
- Automatic task dispatch when an AGV becomes free
- Task queue when more tasks exist than AGVs
- Auto-resume functionality
- Logs for every task start, progress, and completion

This was achieved using:
- Java ExecutorService thread pool
- Asynchronous task execution
- Safe state updates and logging

---

## Features Demonstrated
1. **Parallel Charging Simulation**
   - Multiple AGVs charge at the same time
   - Charging stations run concurrently
   - Waiting queue and timeout mechanism

2. **Random Arrival System**
   - AGVs appear unpredictably
   - Queue management and scheduling

3. **Parallel Task Execution**
   - Multiple tasks processed at once
   - Automatic AGV assignment
   - Real-time task scheduling and resuming

---

## Evidence of Functionality
The system includes the following forms of proof:
- Console logs showing concurrent operations
- Timestamps demonstrating overlapping tasks and charging events
- A recorded demonstration video
- Thread-based execution using ExecutorService

---

## Technologies Used
- **Java**
- **Thread Pool (ExecutorService)**
- **Atomic Variables**
- **Concurrent Processing**
- **Custom Simulation Logic**
- **Logging System**

---

## How to Run
1. Compile the Java project.
2. Run the main application.
3. Use menu options to:
   - Start charging simulation
   - Add new AGVs
   - Create and run tasks
   - Monitor logs

The system will show parallel behavior in real time.

