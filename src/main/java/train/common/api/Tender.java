package train.common.api;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;
import train.common.api.LiquidManager.FilteredTank;
import train.common.api.LiquidManager.StandardTank;

public abstract class Tender extends Freight implements IFluidHandler {

	public ItemStack tenderItems[];
	public int liquidId = 0;
	public int fuelSlot = 1;
	public int waterSlot = 1;
	private int maxTank;
	private int update = 8;
	public FluidTank theTank;
	private FluidStack liquid;

	/**
	 * 
	 * @param world
	 * @param liquidId
	 * @param quantity
	 * @param capacity
	 */
	public Tender(World world, Fluid liquid, int quantity, int capacity) {
		this(new FluidStack(liquid, quantity), capacity, world, null);
	}

	public Tender(World world, Fluid liquid, int quantity, int capacity, FluidStack filter) {
		this(new FluidStack(liquid, quantity), capacity, world, filter);
	}

	private Tender(FluidStack liquid, int capacity, World world, FluidStack filter) {
		super(world);
		this.liquid = liquid;
		this.maxTank = capacity;
		this.theTank = LiquidManager.getInstance().new StandardTank(5000);
	}
	@Override
	public abstract int getSizeInventory();
	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
		super.writeEntityToNBT(nbttagcompound);
		this.theTank.writeToNBT(nbttagcompound);
	}
	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
		super.readEntityFromNBT(nbttagcompound);
		this.theTank.readFromNBT(nbttagcompound);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (worldObj.isRemote)
			return;
	}
	/**
	 * handle mass depending on items and liquid
	 */
	@Override
	protected void handleMass(){
		if(this.updateTicks%10!=0)return;
		double preciseAmount=0;
		this.mass=this.getDefaultMass();
		if(theTank != null && theTank.getFluid() != null && theTank.getFluid().amount>0){
			preciseAmount = theTank.getFluid().amount;
		}		
		this.itemInsideCount=0;
		for (int i = 0; i < getSizeInventory(); i++) {
			ItemStack itemstack = getStackInSlot(i);
			if (itemstack == null) {
				continue;
			}
			if (itemstack.stackSize <= 0) {
				continue;
			}else{
				this.itemInsideCount+=itemstack.stackSize;
			}
		}
		mass+=(this.itemInsideCount*0.0001);//1 item = 1 kilo
		mass+=(preciseAmount/10000);//1 bucket = 1 kilo
	}

	/**
	 * added for SMP, used by the HUD
	 * 
	 * @return
	 */
	public int getWater() {
		if (theTank != null && theTank.getFluid() != null) {
			return theTank.getFluidAmount();
		}
		return 0;
	}

	public int getCartTankCapacity() {
		return maxTank;
	}

	private void placeInInvent(ItemStack itemstack1, Tender tender) {
		for (int i = 1; i < tender.tenderItems.length; i++) {
			if (tender.tenderItems[i] == null) {
				tender.tenderItems[i] = itemstack1;
				return;
			}
			else if (tender.tenderItems[i] != null && tender.tenderItems[i].getItem() == itemstack1.getItem() && itemstack1.isStackable() && (!itemstack1.getHasSubtypes() || tender.tenderItems[i].getItemDamage() == itemstack1.getItemDamage()) && ItemStack.areItemStackTagsEqual(tender.tenderItems[i], itemstack1)) {
				int var9 = tender.tenderItems[i].stackSize + itemstack1.stackSize;
				if (var9 <= itemstack1.getMaxStackSize()) {
					tender.tenderItems[i].stackSize = var9;

				}
				else if (tender.tenderItems[i].stackSize < itemstack1.getMaxStackSize()) {
					tender.tenderItems[i].stackSize += 1;
				}
				return;
			}
			else if (i == tender.tenderItems.length - 1) {
				dropItem(itemstack1.getItem(), 1);
				return;
			}
		}
	}

	public void liquidInSlot(ItemStack itemstack, Tender tender) {
		if (worldObj.isRemote)
			return;
		this.update += 1;
		if (this.update % 8 == 0 && itemstack != null) {
			ItemStack result = LiquidManager.getInstance().processContainer(this, 0, theTank, itemstack);
			if (result != null) {
				placeInInvent(result, tender);
				//decrStackSize(0, 1);
			}
		}
	}

	protected void checkInvent(ItemStack tenderInvent, Tender loco) {
		if (tenderInvent != null) {
			liquidInSlot(tenderInvent, loco);
		}
	}
	/*IInventory implements*/
	@Override
	public ItemStack getStackInSlot(int i) {
		return tenderItems[i];
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int par1) {
		if (this.tenderItems[par1] != null) {
			ItemStack var2 = this.tenderItems[par1];
			this.tenderItems[par1] = null;
			return var2;
		}
		else {
			return null;
		}
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		if (tenderItems[i] != null) {
			if (tenderItems[i].stackSize <= j) {
				ItemStack itemstack = tenderItems[i];
				tenderItems[i] = null;
				return itemstack;
			}
			ItemStack itemstack1 = tenderItems[i].splitStack(j);
			if (tenderItems[i].stackSize == 0) {
				tenderItems[i] = null;
			}
			return itemstack1;
		}
		else {
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		tenderItems[i] = itemstack;
		if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
			itemstack.stackSize = getInventoryStackLimit();
		}
	}
	
	public void setLiquid(FluidStack liquid) {
		this.liquid = liquid;
	}

	public void setCapacity(int capacity) {
		this.maxTank = capacity;
	}

	public int getCapacity() {
		return this.maxTank;
	}
	@Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
    {
        return theTank.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
    {
        if (resource == null || !resource.isFluidEqual(theTank.getFluid()))
        {
            return null;
        }
        return theTank.drain(resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
    {
        return theTank.drain(maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid)
    {
        return true;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid)
    {
        return true;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from)
    {
        return new FluidTankInfo[] { theTank.getInfo() };
    }
	
    public FluidStack getFluid()
    {
        return theTank.getFluid();
    }

    public int getFluidAmount()
    {
        return theTank.getFluidAmount();
    }
}