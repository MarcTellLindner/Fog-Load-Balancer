import sys
import numpy as np
from pyearth import Earth

X_str = sys.argv[1]
X = np.array([np.fromstring(row, sep=',') for row in X_str.split(';')])

y_str = sys.argv[2]
y = np.array([np.fromstring(row, sep=',') for row in y_str.split(';')])

model = Earth()
model.fit(X,y)

formula = ''
for idx in range(len(model.basis_)):
	cur = model.basis_[idx]
	if not cur.is_pruned():
		formula += '+' + str(cur) if str(cur) != '(Intercept)' else str(1.0)
		formula += '*' + str(model.coef_[:, idx])

print(formula)

