AStarSearch functions by using a closed list (implemented with a hash table) to track the nodes that have already been set. We track the open list of potential nodes to visit next through a priority queue, such that each dequeue gives us the cheapest f(x) value. 

We track the best path to each node with pointers to the parent (similar to a linked list). 

Each time we dequeue from the priority queue, we add the "parent node" to the closed list, get all the neighbors, set the "cameFrom" field to the parent, and compute the f(x) by euclidean distance + parent.cost. Finally, we check the closed list to see if the neighbor was already visited. If the neighbor was already visited, and the f(x) of our current neighbor is greater than the previous visit, then we do nothing with our current neighbor. 

In every other case, we add the neighbor to the open list.

To implement this Astar function to make it compatible with hash table and priority queue, we adjusted the MapLocation class by overriding hashcode, equals, and compareTo.

This continues until the open list is empty (in which case the path is impossible) or the goal is reached. 

When the goal is reached, we call the helper method, "tracePath". It continually traces through the cameFrom fields, starting from goal, and adding the cameFrom value to the stack. Conveniently, this backwards order allows the stack to call the steps in the proper order.

ShouldReplan is simple. If the enemy is at the position that the stack specifies is the next step, then it returns true. 

We treat the enemy agent's location as simply another obstacle in path planning, which enables ShouldReplanPath to properly update the AStarSearch to find a new viable path.
      

