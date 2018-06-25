# OnlineRegressor

An Android app that uses the Tango SDK [deprecated] and the ActiVis interface to guide a user to point the device in a given direction. The app tracks the user's performance over time (time to target, path length, etc.) and adjusts the interface's parameters over time to adapt itself to the user's needs, thereby improving performance. 

The adaptation is done using a 2nd or 3rd order online-regression procedure. A possible future improvement can be to implement an Linear Quadratic Regulator (LQR) to drive the interface towards the optimum values.
