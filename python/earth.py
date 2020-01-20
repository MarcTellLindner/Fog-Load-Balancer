import sys, os, sympy, math
import numpy as np
import matplotlib.pyplot as pypl
from pyearth import Earth, export

X_str = sys.argv[1]
X = np.array([np.fromstring(row, sep=',') for row in X_str.split(';')])

y_str = sys.argv[2]
y = np.array([np.fromstring(row, sep=',') for row in y_str.split(';')])

model = Earth(max_degree=3)
model.fit(X,y)

sympy_format = export.export_sympy(model)

# Print as C-Code, because Java is not available
sympy.printing.print_ccode(sympy_format, standard='C89')

# Print RMSE
print(math.sqrt(model.mse_))

if len(sys.argv) > 3: 
	name = sys.argv[3]
	pypl.title(name)
	
	start = min(X)
	stop = max(X)
	step = (stop - start) / 100
	stop += step
	
	x_vals = np.arange(start, stop, step)
	y_vals = model.predict(x_vals[:, None])
	pypl.plot(x_vals, y_vals)
	x_input = np.concatenate(X)
	y_input = np.concatenate(y)
	pypl.scatter(X, y)
	if not os.path.isdir('figs'):
		os.makedirs('figs')
	pypl.savefig('figs/' + name + '.png')
