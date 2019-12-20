import sys, sympy, numpy as np
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
