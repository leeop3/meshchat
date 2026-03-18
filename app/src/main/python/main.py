import RNS
import LXMF

def get_info():
    # Return versions to prove RNS and LXMF are alive
    return f"Mesh Engine Status:\nRNS Version: {RNS.__version__}\nLXMF Version: {LXMF.__version__}"