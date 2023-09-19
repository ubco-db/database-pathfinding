package util;

public class CircularQueue
{
	private int maxSize;
	private int[] data;
	private int front;
	private int rear;
	private int iteratorLoc;
	
	public CircularQueue(int size)
	{	maxSize = size;
		data = new int[maxSize];
		front = -1;
		rear = -1;
		iteratorLoc = -1;
	}
	
	// Removes the first element in the queue
	public int remove()
	{ 		
	     if (front != -1)
	     {  // Must be an element in the queue
	    	 int val = data[front];
	         if (front==rear)
	         {  // Queue is now empty 
	            front = -1;
	            rear = -1;	            
	         }
	         else
	         {
	            if (front==maxSize-1)
	                front=0;
	            else
	                front++;	                     
	         }
	         return val;
	     }     
	     return -1;
	}

	public void initIterator()
	{
		iteratorLoc = front;	    
	}

	/**
	Retrieves the next element using the iterator if it exists.
	Returns 1 if element exists and fills data structure for data record provided in call.
	*/
	public int next()
	{    
	    if (iteratorLoc == -1)
	        return 0;                   // Queue is empty
	    
	    if (iteratorLoc == maxSize)
	    	iteratorLoc = 0;        // Wrapped around in circular queue
	                
	    return data[iteratorLoc++];	      
	}

	/*
	Returns the number of elements in the queue.
	*/
	public int size()
	{
	    if (front == -1)
	        return 0;                                                                                       
	    else if (rear < front)
	        return maxSize-front+rear+1;         // rear pointer is less than front
	    else
	        return rear - front + 1;
	}
	
	public boolean insert(int val)
	{
	    if ( (front==0 && rear==maxSize-1) || (rear+1==front))    
	        return false;   // Queue is full
	    	    	    
	    if (rear==maxSize-1)
	        rear=0;
	    else
	        rear++;             
	            
	    data[rear] = val;
	    
	    if (front==-1)          // Previously empty queue - write both front and rear to external memory
	       front=0;	               	    
	                
	    return true;      
	}
}
