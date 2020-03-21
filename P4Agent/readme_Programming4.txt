For our forward state planner, we used A* search by generating future game states that can only be reached 
by applying STRIP Actions that have their preconditions satisfied.

Our heurstic takes into how much gold and wood is left to be collected and deposited by the peasant. While our cost
takes into account distance and for the harvest and deposit actions we give it a cost 1.

Our STRIPS actions consist of a moveTo action which allows the agent to move to any desired location,
a harvest action which allows it to harvest at either a tree or a goldmine, and a deposit action which
allows it to deposit at townhall. 

We used the Peasant class to copy and keep track of the peasant's location and the amount of each resource it was 
was currently holding

The Resource class was used to keep track of the resource locations and their changing supply of resources 
during the planning phase. i.e it allows us to remove a resource location to not be considered once the
agent followed a plan that depleated all the resources from that location. 

